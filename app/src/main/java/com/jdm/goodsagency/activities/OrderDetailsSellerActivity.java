package com.jdm.goodsagency.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jdm.goodsagency.Constants;
import com.jdm.goodsagency.R;
import com.jdm.goodsagency.adapters.AdapterOrderedItem;
import com.jdm.goodsagency.models.ModelOrderedItem;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsSellerActivity extends AppCompatActivity {

    //ui views
    private ImageButton backBtn, editBtn, mapBtn;
    public String orderCost;
    private TextView orderIdTv, dateTv, orderStatusTv,emailTv, phoneTv, totalItemsTv, amountTv,addressTv;
    private RecyclerView itemsRv;
    String orderId,orderBy;
    String sourceLatitude,sourceLongitude,destinationLatitude,destinationLongitude;
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelOrderedItem> orderedItemArrayList;
    private AdapterOrderedItem adapterOrderedItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details_seller);

        // init ui views
        backBtn=findViewById(R. id.backBtn) ;
        editBtn = findViewById(R. id.editBtn) ;
        mapBtn =findViewById(R.id.mapBtn);
        orderIdTv = findViewById(R.id.orderIdTv);
        dateTv = findViewById(R.id.dateTv);
        orderStatusTv = findViewById(R. id.orderStatusTv) ;
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R. id.phoneTv) ;
        totalItemsTv= findViewById(R.id.totalItemsTv);
        amountTv=findViewById(R. id.amountTv);
        addressTv=findViewById(R.id.addressTv);
        itemsRv=findViewById(R.id.itemsRv);
        //get data from intent
        orderId=getIntent().getStringExtra("orderId");
        orderBy=getIntent().getStringExtra("orderBy");

        firebaseAuth = FirebaseAuth.getInstance();
        loadMyInfo();
        loadBuyerInfo();
        loadOrderDetails();
        loadOrderedItems();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMap();
            }
        });
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editOrderStatusDialog();
            }
        });
    }

    private void editOrderStatusDialog() {
        final String[] options={"In Progress","Completed","Cancelled"};
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Edit Order Status")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String selectedOptions=options[i];
                        editOrderStatus(selectedOptions);
                    }
                })
                .show();
    }

    private void editOrderStatus(String selectedOptions) {
        HashMap<String , Object> hashMap=new HashMap<>();
        hashMap.put("orderStatus",""+selectedOptions);

        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Orders").child(orderId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        String message="Order is now "+selectedOptions;
                        Toast.makeText(OrderDetailsSellerActivity.this, message, Toast.LENGTH_SHORT).show();

                        prepareNotificationMessage(orderId,message);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(OrderDetailsSellerActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadOrderDetails() {
        // load detailed info of this order, based on order id
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref. child( firebaseAuth. getUid ( )).child ( "Orders" ) . child(orderId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String orderBy=""+dataSnapshot.child("orderBy").getValue();
                        orderCost=""+dataSnapshot.child("orderCost").getValue();
                        String orderId=""+dataSnapshot.child("orderId").getValue();
                        String orderStatus=""+dataSnapshot.child("orderStatus").getValue();
                        String orderTime=""+dataSnapshot.child("orderTime").getValue();
                        String orderTo=""+dataSnapshot.child("orderTo").getValue();
                        String latitude=""+dataSnapshot.child("latitude").getValue();
                        String longitude=""+dataSnapshot.child("longitude").getValue();
                        String address=""+dataSnapshot.child("address").getValue();

                        Calendar calendar=Calendar.getInstance();
                        calendar.setTimeInMillis(Long.parseLong(orderTime));
                        String dateFormated= DateFormat.format("dd/MM/yyyy",calendar).toString();

                        if (orderStatus.equals("In Progress")){
                            orderStatusTv.setTextColor(getResources().getColor(R.color.purple_700));
                        }
                        else if (orderStatus.equals("Completed")) {
                            orderStatusTv.setTextColor(getResources().getColor(R.color.colorGreen));
                        }
                        else if (orderStatus.equals("Cancelled")) {
                            orderStatusTv.setTextColor(getResources().getColor(R.color.red));
                        }

                        orderIdTv.setText(orderId);
                        orderStatusTv.setText(orderStatus);
                        amountTv.setText("₹ "+orderCost);
                        dateTv.setText(dateFormated);
                        addressTv.setText(address);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }



    private void loadOrderedItems(){
        orderedItemArrayList=new ArrayList<>();
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Orders").child(orderId).child("Items")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        orderedItemArrayList.clear();
                        for (DataSnapshot ds:dataSnapshot.getChildren()){
                            ModelOrderedItem modelOrderedItem=ds.getValue(ModelOrderedItem.class);
                            orderedItemArrayList.add(modelOrderedItem);
                        }
                        adapterOrderedItem=new AdapterOrderedItem(OrderDetailsSellerActivity.this,orderedItemArrayList);
                        itemsRv.setAdapter(adapterOrderedItem);
                        totalItemsTv.setText(""+dataSnapshot.getChildrenCount());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void openMap() {
        //saddr means source address
        //daddr means destination address
        String address="https://maps.google.com/maps?saddr"+sourceLatitude+","+sourceLongitude+"&daddr"+destinationLatitude+","+destinationLongitude;
        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(address));
        startActivity(intent);
    }
    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Users");
        ref.child( firebaseAuth.getUid( ) )
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        sourceLatitude=""+dataSnapshot.child("latitude").getValue();
                        sourceLongitude=""+dataSnapshot.child("longitude").getValue();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void loadBuyerInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Users");
        ref.child(orderBy)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        destinationLatitude=""+dataSnapshot.child("latitude").getValue();
                        destinationLongitude=""+dataSnapshot.child("longitude").getValue();
                        String email=""+dataSnapshot.child("email").getValue();
                        String phone=""+dataSnapshot.child("phone").getValue();

                        //set info
                        emailTv.setText(email);
                        phoneTv.setText(phone);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void prepareNotificationMessage(String orderId,String message){
        String NOTIFICATION_TOPIC="/topics/"+ Constants.FCM_Topic;
        String NOTIFICATION_TITLE="Your Order"+orderId;
        String NOTIFICATION_MESSAGE=""+message;
        String NOTIFICATION_TYPE="OrderStatusChanged";

        JSONObject notificationJo=new JSONObject();
        JSONObject notificationBodyJo=new JSONObject();
        try{
            notificationBodyJo.put("notificationType",NOTIFICATION_TYPE);
            notificationBodyJo.put("buyerUid",orderBy);
            notificationBodyJo.put("sellerUid",firebaseAuth.getUid());
            notificationBodyJo.put("orderId",orderId);
            notificationBodyJo.put("notificationTitle",NOTIFICATION_TITLE);
            notificationBodyJo.put("notificationMessage",NOTIFICATION_MESSAGE);
            notificationBodyJo.put("to",NOTIFICATION_TOPIC);
            notificationBodyJo.put("data",notificationBodyJo);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        sendFcmNotification(notificationJo);
    }

    private void sendFcmNotification(JSONObject notificationJo) {
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //after placing order open order details page

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String > getHeaders() throws AuthFailureError {
                Map<String,String >headers=new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key="+Constants.FCM_KEY);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }
}