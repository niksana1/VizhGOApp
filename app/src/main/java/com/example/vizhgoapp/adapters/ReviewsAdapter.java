package com.example.vizhgoapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vizhgoapp.R;
import com.example.vizhgoapp.model.Landmark;
import com.example.vizhgoapp.model.Review;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.MyViewHolder> {

    Context context;
    ArrayList<Review> reviewsArrayList;
    private SimpleDateFormat dateFormat;

    public ReviewsAdapter(Context context, ArrayList reviewsArrayList) {
        this.context = context;
        this.reviewsArrayList = reviewsArrayList;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReviewsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.review_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewsAdapter.MyViewHolder holder, int position) {

        Review review = reviewsArrayList.get(position);

        holder.ratingBar.setRating(review.getRating());

        holder.tvReviewText.setText(review.getText());
        holder.tvCreatedBy.setText(review.getCreatedBy());

        if(review.getCreatedAt() != null){
            String formattedDate = dateFormat.format(review.getCreatedAt());
            holder.tvCreatedOnDate.setText(formattedDate);
        }
    }

    @Override
    public int getItemCount() {
        return reviewsArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        RatingBar ratingBar;
        TextView tvCreatedBy, tvCreatedOnDate, tvReviewText;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views from review_item.xml
            ratingBar = itemView.findViewById(R.id.rb_rating);
            tvCreatedBy = itemView.findViewById(R.id.tv_added_by);
            tvCreatedOnDate = itemView.findViewById(R.id.tv_added_on_date);
            tvReviewText = itemView.findViewById(R.id.tv_reviewText);
        }
    }
}

