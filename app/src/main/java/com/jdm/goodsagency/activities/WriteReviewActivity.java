package com.jdm.goodsagency.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jdm.goodsagency.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class WriteReviewActivity extends AppCompatActivity {

    //ui views
    private ImageButton backBtn;
    private ImageView profileTv;
    private TextView shopNameTv;
    private RatingBar ratingBar;
    private EditText reviewEt;
    private FloatingActionButton submitBtn;
    private String shopUid;

    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        //init ui views
        backBtn=findViewById(R.id.backBtn);
        profileTv=findViewById(R.id.profileIv);
        shopNameTv=findViewById(R.id.shopNameTv);
        ratingBar=findViewById(R.id.ratingBar);
        reviewEt=findViewById(R.id.reviewEt);
        submitBtn=findViewById(R.id.submitBtn);
        //get shop uid from intent
        shopUid=getIntent().getStringExtra("shopUid");

        firebaseAuth=FirebaseAuth.getInstance();
        //load shop info: shop name,shop image
        loadShopInfo();
        //if user has written review to this shop, load it
        loadReview();

        //go back to previous activity
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //input data
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputData();
            }
        });
    }

    private void loadShopInfo() {
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get shop info
                String shopName=""+dataSnapshot.child("shopName").getValue();
                String shopImage=""+dataSnapshot.child("profileImage").getValue();

                //set shop info to ui
                shopNameTv.setText(shopName);
                try {
                    Picasso.get().load(shopImage).placeholder(R.drawable.ic_store_gray).into(profileTv);
                }
                catch(Exception e){
                    profileTv.setImageResource(R.drawable.ic_store_gray);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadReview() {
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Ratings").child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            //my review is available in this shop

                            //get review details
                            String uid=""+dataSnapshot.child("uid").getValue();
                            String ratings=""+dataSnapshot.child("ratings").getValue();
                            String review=""+dataSnapshot.child("review").getValue();
                            String timestamp=""+dataSnapshot.child("timestamp").getValue();

                            //set review details to our ui
                            float myRating=Float.parseFloat(ratings);
                            ratingBar.setRating(myRating);
                            reviewEt.setText(review);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void inputData() {
        String ratings=""+ratingBar.getRating();
        String review = reviewEt.getText().toString().trim();

        //for time of review
        String timestamp =""+System.currentTimeMillis();

        //setup data in hashmap
        HashMap<String, Object> hashMap=new HashMap<>();
        hashMap.put("uid",""+firebaseAuth.getUid());
        hashMap.put("ratings",""+ratings); //e.g. 4.6
        hashMap.put("review",""+ review); //e.g. Good service
        hashMap.put("timestamp",""+ timestamp);

        //put to db: DB > Users > ShopUid > Ratings
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Ratings").child(firebaseAuth.getUid()).updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //review added to db
                        Toast.makeText(WriteReviewActivity.this,"Review published successfully...",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed adding review to db
                        Toast.makeText(WriteReviewActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}