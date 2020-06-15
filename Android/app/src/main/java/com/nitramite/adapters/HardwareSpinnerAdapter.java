package com.nitramite.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.nitramite.nitspec.R;
import java.util.ArrayList;

public class HardwareSpinnerAdapter extends ArrayAdapter<HardwareItem> {

    // Variables
    private ArrayList<HardwareItem> hardwareItems;
    private LayoutInflater inflater;

    // Constructor
    public HardwareSpinnerAdapter(Context context, ArrayList<HardwareItem> hardwareItems_) {
        super(context, R.layout.hardware_item, hardwareItems_);
        // TODO Auto-generated constructor stub
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.hardwareItems = hardwareItems_;
    }


    @SuppressWarnings("NullableProblems")
    public View getView(int position, View convertView, ViewGroup parent) {
        @SuppressLint("ViewHolder") View itemView = inflater.inflate(R.layout.hardware_item, parent, false);

        // Find views
        TextView itemLetter = itemView.findViewById(R.id.itemLetter);
        TextView itemName = itemView.findViewById(R.id.itemName);
        TextView itemDescription = itemView.findViewById(R.id.itemDescription);

        // Set content
        itemLetter.setText(hardwareItems.get(position).getItemLetter());
        itemName.setText(hardwareItems.get(position).getItemName());
        itemDescription.setText(hardwareItems.get(position).getItemDescription());

        return itemView;
    }


    @SuppressWarnings("NullableProblems")
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);

    }


} // End of class