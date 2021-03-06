package eu.spod.isislab.spodapp.fragments;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.BitmapCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import eu.spod.isislab.spodapp.R;
import eu.spod.isislab.spodapp.services.SpodLocationServices;
import eu.spod.isislab.spodapp.utils.AddressSolver;
import eu.spod.isislab.spodapp.utils.DownloadImageTask;
import eu.spod.isislab.spodapp.utils.NetworkChannel;
import eu.spod.isislab.spodapp.utils.User;

/**
 * Created by Utente on 07/07/2017.
 */
public class GalleryAddItemFragment extends Fragment implements View.OnClickListener, BottomNavigationView.OnNavigationItemSelectedListener, Observer{

    private static final int PHOTO_REQUEST_CODE = 1;
    private ImageView image;
    ViewGroup rootView;
    Bitmap bp = null;
    private Uri mImageUri;

    String sheetId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        rootView = (ViewGroup) inflater.inflate(R.layout.gallery_add_item_fragment, container, false);
        /*TextView location = (TextView)rootView.findViewById(R.id.new_item_position);
        if(SpodLocationServices.getCurrentLocation() != null)
           new AddressSolver(location, getActivity()).execute(SpodLocationServices.getCurrentLocation());*/

        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                rootView.findViewById(R.id.add_item_bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        new DownloadImageTask((ImageView)rootView.findViewById(R.id.new_item_avatar))
                .execute(User.getInstance().getAvatarImage());

        image = (ImageView)rootView.findViewById(R.id.new_item_image);
        image.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getPhoto();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==PHOTO_REQUEST_CODE && resultCode == getActivity().RESULT_OK)
        {
            /*bp = (Bitmap) data.getExtras().get("data");
            image.setImageBitmap(bp);
            image.setScaleType(ImageView.ScaleType.FIT_XY);*/
            this.grabImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        NetworkChannel.getInstance().deleteObserver(this);
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        getPhoto();
    }

    public void grabImage(ImageView imageView)
    {
        getActivity().getContentResolver().notifyChange(mImageUri, null);
        ContentResolver cr = getActivity().getContentResolver();
        try
        {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager)getActivity(). getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);

            //bp = android.provider.MediaStore.Images.Media.getBitmap(cr, mImageUri);
            String x = mImageUri.getPath();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            bp = BitmapFactory.decodeFile(mImageUri.getPath(), options);

            if(bp.getByteCount() > mi.availMem){
                options = new BitmapFactory.Options();
                options.inSampleSize = 8;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bp.compress(Bitmap.CompressFormat.JPEG, 0 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();
                ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
                Bitmap preview_bitmap = BitmapFactory.decodeStream(bs, null, options);

                imageView.setImageBitmap(preview_bitmap);
            }else {
                imageView.setImageBitmap(bp);
            }
        }
        catch (Exception e)
        {
            Snackbar.make(rootView, "Failed to load the image!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void getPhoto()
    {
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo;
        try
        {
            // place where to store camera taken picture
            photo = this.createTemporaryFile("picture", ".jpg");
            photo.delete();

            mImageUri = Uri.fromFile(photo);
            photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
            startActivityForResult(photoIntent, PHOTO_REQUEST_CODE);
        }
        catch(Exception e)
        {
            Snackbar.make(rootView, "Please check SD card!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

    }

    private File createTemporaryFile(String part, String ext) throws Exception
    {
        File tempDir= Environment.getExternalStorageDirectory();
        tempDir=new File(tempDir.getAbsolutePath() + "/.temp/");
        if(!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        return File.createTempFile(part, ext, tempDir);
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bottom_bar_add_item_upload:
                String title       = ((TextView)rootView.findViewById(R.id.new_item_title)).getText().toString();
                String description = ((TextView)rootView.findViewById(R.id.new_item_description)).getText().toString();
                Location location  = SpodLocationServices.getCurrentLocation();

                if(title.isEmpty() || description.isEmpty() || location == null){
                    Snackbar.make(getActivity().findViewById(R.id.container), "Please fill form correctly, maybe the location is not accessible, check it!!!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }else{
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    String strDate = sdf.format(c.getTime());

                    NetworkChannel.getInstance().addObserver(this);
                    NetworkChannel.getInstance().addRowToSheet(sheetId, title, description, strDate, bp);
                }

                break;
        }
        return true;
    }

    @Override
    public void update(Observable o, Object response)
    {
        try {
            JSONObject result = new JSONObject((String)response);
            Boolean status = result.getBoolean("status");
            String message = result.getString("message");

            if(status){
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                NetworkChannel.getInstance().deleteObserver(this);
                CocreationRoomFragment roomFragment = (CocreationRoomFragment)getActivity().getSupportFragmentManager().findFragmentByTag("cocreation_room");
                roomFragment.refreshData();

                this.getActivity().getSupportFragmentManager().popBackStack();

            }else{
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
