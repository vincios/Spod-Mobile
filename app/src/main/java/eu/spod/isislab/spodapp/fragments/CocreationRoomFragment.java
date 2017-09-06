package eu.spod.isislab.spodapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;

import java.util.Observable;
import java.util.Observer;

import eu.spod.isislab.spodapp.MainActivity;
import eu.spod.isislab.spodapp.adapters.MediaGalleryScreenSlidePagerAdapter;
import eu.spod.isislab.spodapp.utils.NetworkChannel;
import eu.spod.isislab.spodapp.R;
import eu.spod.isislab.spodapp.utils.ZoomOutPageTransformer;

public class CocreationRoomFragment extends Fragment implements Observer {

    String roomName;
    String roomId;
    String sheetId;

    JSONArray response;

    public CocreationRoomFragment(){}

    public void setRoom(String roomName, String roomId, String sheetId){

        this.roomName = roomName;
        this.roomId   = roomId;
        this.sheetId  = sheetId;
    }


    public void refreshData(){
        NetworkChannel.getInstance().addObserver(this);
        NetworkChannel.getInstance().getSheetData(roomId);
    }

    @Override
    public void onPause() {
        NetworkChannel.getInstance().deleteObserver(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        NetworkChannel.getInstance().getSheetData(roomId);
        super.onResume();
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            response = (JSONArray) arg;
        }catch (Exception e){
            e.printStackTrace();
        }
        NetworkChannel.getInstance().deleteObserver(this);
    }
}
