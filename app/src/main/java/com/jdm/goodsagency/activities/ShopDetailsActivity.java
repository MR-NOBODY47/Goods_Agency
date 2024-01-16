package com.jdm.goodsagency.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
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
import com.jdm.goodsagency.adapters.AdapterCartItem;
import com.jdm.goodsagency.adapters.AdapterProductUser;
import com.jdm.goodsagency.adapters.AdapterReview;
import com.jdm.goodsagency.models.ModelCartItem;
import com.jdm.goodsagency.models.ModelProduct;
import com.jdm.goodsagency.models.ModelReview;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class ShopDetailsActivity extends AppCompatActivity {

    //declare ui views
    private ImageView shopIv;
    private TextView shopNameTv,phoneTv,emailTv,openCloseTv,deliveryFeeTv,addressTv,filteredProductsTv,
            cartCountTv;
    private ImageButton callBtn,mapBtn,cartBtn,backBtn,filterProductBtn,reviewsBtn;
    private EditText searchProductEt;
    private RecyclerView productsRv;
    private RatingBar ratingBar;

    private String shopUid;
    private String myPhone,userAddress;
    private String shopName,shopEmail,shopPhone,shopAddress,shopLatitude,shopLongitude;
    public String deliveryFee;

    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    private ArrayList<ModelProduct> productsList;
    private AdapterProductUser adapterProductUser;

    //cart
    private ArrayList<ModelCartItem> cartItemList;
    private AdapterCartItem adapterCartItem;
    private EasyDB easyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        //init ui views
        shopIv=findViewById(R.id.shopIv);
        shopNameTv=findViewById(R.id.shopNameTv);
        phoneTv=findViewById(R.id.phoneTv);
        emailTv=findViewById(R.id.emailTv);
        openCloseTv=findViewById(R.id.openCloseTv);
        deliveryFeeTv=findViewById(R.id.deliveryFeeTv);
        addressTv=findViewById(R.id.addressTv);
        filteredProductsTv=findViewById(R.id.filteredProductsTv);
        callBtn=findViewById(R.id.callBtn);
        mapBtn=findViewById(R.id.mapBtn);
        cartBtn=findViewById(R.id.cartBtn);
        backBtn=findViewById(R.id.backBtn);
        filterProductBtn=findViewById(R.id.filterProductBtn);
        searchProductEt=findViewById(R.id.searchProductEt);
        productsRv=findViewById(R.id.productsRv);
        cartCountTv=findViewById(R.id.cartCountTv);
        reviewsBtn=findViewById(R.id.reviewsBtn);
        ratingBar= findViewById(R.id.ratingBar);

        //init progress dialog
        progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        shopUid=getIntent().getStringExtra("shopUid");
        userAddress=getIntent().getStringExtra("useraddress");
        firebaseAuth=FirebaseAuth.getInstance();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();
        loadReviews();//avg rating, set on ratingbar

        //declare it to class level and init in onCreate
        easyDB=EasyDB.init(this,"ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id",new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Name",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price_Each",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Quantity",new String[]{"text","not null"}))
                .doneTableColumn();

        //each shop have its own products and orders so if user add items to cart and go back and open cart in different shop
        deleteCartData();
        cartCount();
        //search
        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProductUser.getFilter().filter(s);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //get uid of the shop from intent
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //go previous activity
                onBackPressed();
            }
        });
        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show cart dialog
                showCartDialog();
            }
        });
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dailPhone();
            }
        });
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMap();
            }
        });
        filterProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder=new AlertDialog.Builder(ShopDetailsActivity.this);
                builder.setTitle("Choose Category:")
                        .setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //get selected item
                                String selected=Constants.productCategories1[which];
                                filteredProductsTv.setText(selected);
                                if (selected.equals("All")){
                                    //load all
                                    loadShopProducts();
                                }
                                else {
                                    //load filtered
                                    adapterProductUser.getFilter().filter(selected);
                                }
                            }
                        })
                        .show();
            }
        });

        //handle reviewBtn click, open reviews activity
        reviewsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //pass shop uid to show its reviews
                Intent intent=new Intent(ShopDetailsActivity.this,ShopReviewsActivity.class);
                intent.putExtra("shopUid",shopUid);
                startActivity(intent);
            }
        });

    }

    private float ratingSum=0;
    private void loadReviews() {
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).child("Ratings")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //clear list before adding data into it
                        ratingSum=0;
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            float rating=Float.parseFloat(""+ds.child("ratings").getValue());
                            ratingSum=ratingSum +rating; //for avg rating, add all ratings, later will divide it by number of reviews

                        }


                        long numberOfReviews=dataSnapshot.getChildrenCount();
                        float avgRating=ratingSum/numberOfReviews;
                        ratingBar.setRating(avgRating);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void deleteCartData() {
        easyDB.deleteAllDataFromTable();//delete all records from cart
    }

    public void cartCount(){
        //keep it public  so we can access in adapter
        //get cart count
        int count=easyDB.getAllData().getCount();
        if(count<=0){
            //no item in cart,hide cart count textview
            cartCountTv.setVisibility(View.GONE);
        }
        else {
            //have items in cart, show cart count textview and set count
            cartCountTv.setVisibility(View.VISIBLE);
            cartCountTv.setText(""+count);//concatenate with string, because we cant set integer in textview
        }
    }

    public double allTotalPrice=0.00;
    //need to access these views in adapter so making public
    public TextView sTotalTv,dFeeTv,allTotalPriceTv;
    private void showCartDialog() {
        //init list
        cartItemList=new ArrayList<>();
        //inflate cart layout
        View view= LayoutInflater.from(this).inflate(R.layout.dialog_cart,null);
        //init views
        TextView shopNameTv=view.findViewById(R.id.shopNameTv);
        sTotalTv=view.findViewById(R.id.sTotalTv);
        dFeeTv=view.findViewById(R.id.dFeeTv);
        allTotalPriceTv=view.findViewById(R.id.totalTv);
        RecyclerView cartItemsRv=view.findViewById(R.id.cartItemsRv);
        Button checkoutBtn=view.findViewById(R.id.checkoutBtn);

        //dialog
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        //set view to dialog
        builder.setView(view);
        shopNameTv.setText(shopName);

        EasyDB easyDB=EasyDB.init(this,"ITEMS_DB")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id",new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Name",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price_Each",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Price",new String[]{"text","not null"}))
                .addColumn(new Column("Item_Quantity",new String[]{"text","not null"}))
                .doneTableColumn();

        //get all records from db
        Cursor res=easyDB.getAllData();
        while (res.moveToNext()){
            String id=res.getString(1);
            String pId=res.getString(2);
            String name=res.getString(3);
            String price=res.getString(4);
            String cost=res.getString(5);
            String quantity=res.getString(6);

            allTotalPrice=allTotalPrice+Double.parseDouble(cost);

            ModelCartItem modelCartItem=new ModelCartItem(
                    ""+id,
                    ""+pId,
                    ""+name,
                    ""+price,
                    ""+cost,
                    ""+quantity);
            cartItemList.add(modelCartItem);
        }
        //setup adapter
        adapterCartItem=new AdapterCartItem(this,cartItemList);
        //set to recyclerView
        cartItemsRv.setAdapter(adapterCartItem);

        dFeeTv.setText("₹ "+deliveryFee);
        sTotalTv.setText("₹ "+String.format("%.2f",allTotalPrice));
        allTotalPriceTv.setText("₹"+(allTotalPrice+Double.parseDouble(deliveryFee.replace("₹",""))));

        //show dialog
        AlertDialog dialog=builder.create();
        dialog.show();

        //reset total price on dialog dismiss
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                allTotalPrice=0.00;
            }
        });

        //place order
        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //first validate delivery address
                if (userAddress.equals("")||userAddress.equals("null")){
                    //user didn't enter address in profile
                    Toast.makeText(ShopDetailsActivity.this, "Please enter your address in your profile before placing order...", Toast.LENGTH_SHORT).show();
                    return;//don't proceed further
                }
                if (myPhone.equals("")||myPhone.equals("null")){
                    //user didn't enter phone number in profile
                    Toast.makeText(ShopDetailsActivity.this, "Please enter your phone number in your profile before placing order...", Toast.LENGTH_SHORT).show();
                    return;//don't proceed further
                }
                if (cartItemList.size()==0){
                    //cart list is empty
                    Toast.makeText(ShopDetailsActivity.this, "No item in cart", Toast.LENGTH_SHORT).show();
                    return;//don't proceed further
                }
                submitOrder();
            }
        });
    }

    private void submitOrder() {
        //show progress dialog
        progressDialog.setMessage("Placing Order...");
        progressDialog.show();

        //for order id and order time
        String timestamp=""+System.currentTimeMillis();

        String cost=allTotalPriceTv.getText().toString().trim().replace("₹","");//remove if ₹ contains

        //setup order data
        HashMap<String,String> hashMap=new HashMap<>();
        hashMap.put("orderId",""+timestamp);
        hashMap.put("orderTime",""+timestamp);
        hashMap.put("orderStatus","In Progress");//in progress/complete/cancelled
        hashMap.put("orderCost",""+cost);
        hashMap.put("orderBy",""+firebaseAuth.getUid());
        hashMap.put("orderTo",""+shopUid);
        hashMap.put("address",""+userAddress);



        //add to db
        DatabaseReference ref=FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //order info added now add order items
                        for (int i=0; i<cartItemList.size();i++){
                            String pId=cartItemList.get(i).getpId();
                            String id=cartItemList.get(i).getId();
                            String cost=cartItemList.get(i).getCost();
                            String name=cartItemList.get(i).getName();
                            String price=cartItemList.get(i).getPrice();
                            String quantity=cartItemList.get(i).getQuantity();

                            HashMap<String,String>hashMap1=new HashMap<>();
                            hashMap1.put("pId",pId);
                            hashMap1.put("name",name);
                            hashMap1.put("cost",cost);
                            hashMap1.put("price",price);
                            hashMap1.put("quantity",quantity);

                            ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                        }
                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, "Order Placed Successfully...", Toast.LENGTH_SHORT).show();

                        prepareNotificationMessage(timestamp);


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed placing order
                        progressDialog.dismiss();
                        Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }//now lets show the placed orders,create row_order_user.xml for recyclerview

    private void openMap() {
        //saddr means source address
        //daddr means destination address
        String address="https://maps.google.com/maps?saddr"+userAddress+"&daddr"+shopLatitude+","+shopLongitude;
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(address));
        startActivity(intent);
    }

    private void dailPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+Uri.encode(shopPhone))));
        Toast.makeText(this, ""+shopPhone, Toast.LENGTH_SHORT).show();
    }

    private void loadMyInfo() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot ds: dataSnapshot.getChildren()){
                            //get user data
                            String name=""+ds.child("name").getValue();
                            String email=""+ds.child("email").getValue();
                            myPhone=""+ds.child("phone").getValue();
                            String profileImage=""+ds.child("profileImage").getValue();
                            String accountType=""+ds.child("accountType").getValue();
                            String city=""+ds.child("city").getValue();
                            userAddress=""+ds.child("address").getValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadShopDetails() {
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("Users");
        ref.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //get shop data
                    String name=""+dataSnapshot.child("name").getValue();
                    shopName=""+dataSnapshot.child("shopName").getValue();
                    shopEmail=""+dataSnapshot.child("email").getValue();
                    shopPhone=""+dataSnapshot.child("phone").getValue();
                    shopAddress=""+dataSnapshot.child("address").getValue();
                    shopLatitude=""+dataSnapshot.child("latitude").getValue();
                    shopLongitude=""+dataSnapshot.child("longitude").getValue();
                    deliveryFee=""+dataSnapshot.child("deliveryFee").getValue();
                    String profileImage=""+dataSnapshot.child("profileImage").getValue();
                    String shopOpen=""+dataSnapshot.child("shopOpen").getValue();

                    //set data
                    shopNameTv.setText(shopName);
                    emailTv.setText(shopEmail);
                    deliveryFeeTv.setText("Delivery Fee:₹ "+deliveryFee);
                    addressTv.setText(shopAddress);
                    phoneTv.setText(shopPhone);
                    if (shopOpen.equals("true")){
                        openCloseTv.setText("Open");
                    }
                    else {
                        openCloseTv.setText("Closed");
                    }
                    try {
                        Picasso.get().load(profileImage).into(shopIv);
                    }
                    catch (Exception e){

                    }

                }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadShopProducts() {
        //init list
        productsList=new ArrayList<>();
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //clear list before adding items
                        productsList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            ModelProduct modelProduct=ds.getValue(ModelProduct.class);
                            productsList.add(modelProduct);
                        }
                        //setup adapter
                        adapterProductUser=new AdapterProductUser(ShopDetailsActivity.this,productsList);
                        //set adapter
                        productsRv.setAdapter(adapterProductUser);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void prepareNotificationMessage(String orderId){
        String NOTIFICATION_TOPIC="/topics/"+Constants.FCM_Topic;
        String NOTIFICATION_TITLE="New Order"+orderId;
        String NOTIFICATION_MESSAGE="Congratulations...! You have new order.";
        String NOTIFICATION_TYPE="NewOrder";

        JSONObject notificationJo=new JSONObject();
        JSONObject notificationBodyJo=new JSONObject();
        try{
            notificationBodyJo.put("notificationType",NOTIFICATION_TYPE);
            notificationBodyJo.put("buyerUid",firebaseAuth.getUid());
            notificationBodyJo.put("sellerUid",shopUid);
            notificationBodyJo.put("orderId",orderId);
            notificationBodyJo.put("notificationTitle",NOTIFICATION_TITLE);
            notificationBodyJo.put("notificationMessage",NOTIFICATION_MESSAGE);
            notificationBodyJo.put("to",NOTIFICATION_TOPIC);
            notificationBodyJo.put("data",notificationBodyJo);
        }
        catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        sendFcmNotification(notificationJo,orderId);
    }

    private void sendFcmNotification(JSONObject notificationJo, String orderId) {
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                //after placing order open order details page
                Intent intent=new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo",shopUid);
                intent.putExtra("orderId",orderId);
                startActivity(intent);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Intent intent=new Intent(ShopDetailsActivity.this, OrderDetailsUsersActivity.class);
                intent.putExtra("orderTo",shopUid);
                intent.putExtra("orderId",orderId);
                startActivity(intent);
            }
        }){
            @Override
            public Map<String, String >getHeaders() throws AuthFailureError{
                Map<String,String >headers=new HashMap<>();
                headers.put("Content-Type","application/json");
                headers.put("Authorization","key="+Constants.FCM_KEY);
                return headers;
            }
        };
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

}