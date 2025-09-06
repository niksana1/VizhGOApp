package com.example.vizhgoapp.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.vizhgoapp.model.ItemSelectListener;
import com.example.vizhgoapp.R;
import com.example.vizhgoapp.model.Landmark;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LandmarksAdapter extends RecyclerView.Adapter<LandmarksAdapter.MyViewHolder> {

    Context context;
    ArrayList<Landmark> landmarksArrayList;
    private SimpleDateFormat dateFormat;
    private ItemSelectListener clickListener;

    public LandmarksAdapter(Context context, ArrayList<Landmark> landmarksArrayList, ItemSelectListener clickListener) {
        this.context = context;
        this.landmarksArrayList = landmarksArrayList;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        this.clickListener = clickListener;
    }


    @NonNull
    @Override
    public LandmarksAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LandmarksAdapter.MyViewHolder holder, int position) {

        Landmark landmark = landmarksArrayList.get(position);

        // Set information
        holder.tvLandmarkName.setText(landmark.getName());

        // Translate category name
        String translatedCategory = getTranslatedCategoryName(landmark.getCategory());
        holder.tvCategory.setText(translatedCategory);
        holder.tvAvgRating.setText(landmark.getRatingDisplay());

        // Format date
        if (landmark.getCreatedAt() != null){
            holder.tvDate.setText(dateFormat.format(landmark.getCreatedAt()));
        } else {
            holder.tvDate.setText("N/A");
        }
        // Load image with Glide
        loadImage(landmark.getPictureUrl(), holder.ivLandmarkPicture);

        // Set up click listener
        holder.itemCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClicked(landmarksArrayList.get(position));
            }
        });

    }

    private void loadImage(String imageUrl, ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .transform(new RoundedCorners(16))
                .fitCenter();

        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);
    }

    private String getTranslatedCategoryName(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return context.getString(R.string.missing_category); // Add this string resource
        }

        // Use the same translation logic as CategoryDropdownAdapter
        int stringId = context.getResources().getIdentifier("category_" + categoryKey,
                "string", context.getPackageName());

        if (stringId != 0) {
            return context.getString(stringId);
        } else {
            Log.w("LandmarksAdapter", "Translation not found for category: " + categoryKey);
            return categoryKey; // Fallback to key if translation not found
        }
    }


    @Override
    public int getItemCount() {
        return landmarksArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView tvLandmarkName, tvCategory, tvAvgRating, tvDate;
        ImageView ivLandmarkPicture;
        public CardView itemCardView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLandmarkName = itemView.findViewById(R.id.tv_landmark_name);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvAvgRating = itemView.findViewById(R.id.tv_avg_rating);
            tvDate = itemView.findViewById(R.id.tv_added_on_date);
            ivLandmarkPicture = itemView.findViewById(R.id.iv_landmark_image);
            itemCardView = itemView.findViewById(R.id.cv_landmarkItem_container);
        }

    }
}
