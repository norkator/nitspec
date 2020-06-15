package com.nitramite.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.nitramite.nitspec.R;
import java.util.List;

public class DeviceItemsAdapter extends RecyclerView.Adapter<DeviceItemsAdapter.MyViewHolder> {

    private List<BluetoothDeviceItem> deviceItemList;


    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView deviceName, deviceMACAddress;


        MyViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.image);
            deviceName = view.findViewById(R.id.deviceName);
            deviceMACAddress = view.findViewById(R.id.deviceMACAddress);
        }
    }

    public DeviceItemsAdapter(List<BluetoothDeviceItem> substanceItems) {
        this.deviceItemList = substanceItems;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        BluetoothDeviceItem deviceItem = deviceItemList.get(position);
        holder.deviceName.setText(deviceItem.getDeviceName());
        holder.deviceMACAddress.setText(deviceItem.getDeviceMacAddress());
    }

    @Override
    public int getItemCount() {
        return deviceItemList.size();
    }

} // End of class
