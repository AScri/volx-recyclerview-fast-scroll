package com.volcaniccoder.volxfastscroll;

import android.graphics.Color;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class VolxAdapter extends RecyclerView.Adapter<VolxAdapter.ViewHolder> {

    private List<VolxCharModel> mDataset;
    private VolxAdapterFeatures mFeatures;

    public VolxAdapter(List<VolxCharModel> mDataset, VolxAdapterFeatures mFeatures) {
        this.mFeatures = mFeatures;
        this.mDataset = mDataset;
    }

    @Override
    public VolxAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_char, parent, false);

        return new VolxAdapter.ViewHolder(v, mFeatures);
    }

    @Override
    public void onBindViewHolder(final VolxAdapter.ViewHolder holder, final int position) {

        holder.charText.setBackgroundColor(Color.TRANSPARENT);

        holder.charText.setText(mDataset.get(position).getCharacter().toString().toUpperCase());


        if (mDataset.get(position).isBlink()) {
            holder.charText.setBackgroundColor(mFeatures.getActiveColor());
        }

        if (mDataset.get(position).isGone()) {
            holder.itemParent.setVisibility(View.GONE);
        }

    }

    public void clearBlinks() {
        for (VolxCharModel model : mDataset) {
            model.setBlink(false);
        }
    }

    public VolxCharModel getCharListModelAt(int position) {
        return mDataset.get(position);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView charText;
        private LinearLayout itemParent;

        public ViewHolder(View v, VolxAdapterFeatures mFeatures) {
            super(v);

            LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, mFeatures.getParamsHeight());
            parentParams.setMargins(4, 0, 4, 0);

            itemParent = v.findViewById(R.id.item_parent);
            itemParent.setBackgroundColor(Color.TRANSPARENT);
            itemParent.setLayoutParams(parentParams);

            charText = new AppCompatTextView(v.getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, mFeatures.getParamsHeight());
            charText.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

            if (mFeatures.getTextSize() == Volx.FIT_NICELY) {
                TextViewCompat.setAutoSizeTextTypeWithDefaults(charText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(charText, 2, 13, 1, TypedValue.COMPLEX_UNIT_SP);
            } else
                charText.setTextSize(mFeatures.getTextSize());

            charText.setTextColor(mFeatures.getTextColor());
            charText.setBackgroundColor(Color.TRANSPARENT);

            itemParent.addView(charText, layoutParams);

        }
    }
}
