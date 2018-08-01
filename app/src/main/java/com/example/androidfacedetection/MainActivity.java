package com.example.androidfacedetection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private final static String KEY_IMAGE_PATH = "MainActivity.mImagePath";
    private final static String KEY_IMAGE_SIZE_WIDTH = "MainActivity.mImageSize.width";
    private final static String KEY_IMAGE_SIZE_HEIGHT = "MainActivity.mImageSize.height";
    private final static String KEY_THUMB_SIZE = "MainActivity.thumbSize";
    private final static String KEY_MODE = "MainActivity.mode";
    private final static String KEY_LANDMARKS = "MainActivity.mLandmarks";
    private final static String KEY_CLASSIFICATION_TYPE = "MainActivity.mClassificationType";

    private static final int RQS_LOADIMAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;

    private Button btnLoad, btnDetFace;
    private ImageView imgView;
    private TextView textView;
    
    private SizeT mImageSize;
    private String mImagePath;
    int mThumbSize = 360;
    int mMode = FaceDetector.FAST_MODE;
    int mLandmarks = FaceDetector.NO_LANDMARKS;
    int mClassificationType = FaceDetector.ALL_CLASSIFICATIONS;

//
//    private enum MenuCode {
//
//        MENU_ITEM_FAST( 1 ),
//        MENU_ITEM_ACCURATE( 2 ),
//        MENU_ITEM_NO_LANDMARKS( 3 ),
//        MENU_ITEM_ALL_LANDMARKS( 4 ),
//        MENU_ITEM_NO_CLASSIFICATIONS( 5 ),
//        MENU_ITEM_ALL_CLASSIFICATIONS( 6 ),
//        MENU_ITEM_360( 7 ),
//        MENU_ITEM_640( 8 ),
//        MENU_ITEM_720( 9 ),
//        MENU_ITEM_1080( 10 ),
//        MENU_ITEM_2160( 11 ),
//        MENU_ITEM_DETECT_MODE( 12 ),
//        MENU_ITEM_CLASSIFICATIONS( 13 ),
//        MENU_ITEM_LANDMARKS( 14 ),
//        MENU_ITEM_THUMB_SIZE( 15 );
//
//        private int mCode;
//
//        MenuCode( int code ){
//
//            mCode = code;
//        }
//
//        public int getCode() {
//
//            return mCode;
//        }
//    }

    private Bitmap myBitmap;
    FaceDetector faceDetector;



    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        btnLoad = ( Button ) findViewById( R.id.btnLoad );
        btnDetFace = ( Button ) findViewById( R.id.btnDetectFace );
        imgView = ( ImageView ) findViewById( R.id.imgview );
        textView = ( TextView ) findViewById( R.id.textView );

        if( savedInstanceState != null && mThumbSize == 0 ){

            mImagePath = savedInstanceState.getString( KEY_IMAGE_PATH );
            mImageSize = new SizeT(  );
            mImageSize.width = savedInstanceState.getInt( KEY_IMAGE_SIZE_WIDTH );
            mImageSize.height = savedInstanceState.getInt( KEY_IMAGE_SIZE_HEIGHT );
            mThumbSize = savedInstanceState.getInt( KEY_THUMB_SIZE );
            mMode = savedInstanceState.getInt( KEY_MODE );
            mLandmarks = savedInstanceState.getInt( KEY_LANDMARKS );
            mClassificationType = savedInstanceState.getInt( KEY_CLASSIFICATION_TYPE );
        }


        btnLoad.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {

           onLoad();
            }
        } );

        btnDetFace.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {

                textView.setText( "" );

                if ( myBitmap == null ) {

                    Toast.makeText( MainActivity.this, "myBitmap == null", Toast.LENGTH_LONG ).show();
                } else {
                    detectFace();
                }
            }
        } );

    }

    private void onLoad()
    {
        boolean hasPermission = true;

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {

            if( ContextCompat.checkSelfPermission( MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED )
                hasPermission = false;

            if( hasPermission == false ){

                ActivityCompat.requestPermissions( MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE },
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            }
        }

        if( hasPermission )
            requestImage();
    }

    private void requestImage(){

        Intent intent = new Intent();
        intent.setType( "image/*" );
        intent.setAction( Intent.ACTION_GET_CONTENT );
        intent.addCategory( Intent.CATEGORY_OPENABLE );
        startActivityForResult( intent, RQS_LOADIMAGE );
        textView.setText( "" );
    }


    @Override
    protected void onRestoreInstanceState( Bundle savedInstanceState ) {

        super.onRestoreInstanceState( savedInstanceState );


    }

    @Override
    protected void onPause() {

        faceDetector.release();
        faceDetector = null;
        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if ( faceDetector == null ) {

          loadDetector();
        }
    }

    private void loadDetector(){

        faceDetector = new FaceDetector.Builder( getApplicationContext() )
                .setMode( mMode )
                .setLandmarkType( mLandmarks )
                .setClassificationType( mClassificationType )
                .setTrackingEnabled( false )
                .build();
    }

    public static boolean isMediaDocument( Uri uri ) {

        return "com.android.providers.media.documents".equals( uri
                .getAuthority() );
    }

    public String getFileName( Uri uri ){

        if( isMediaDocument( uri )){

            final String docId = DocumentsContract.getDocumentId( uri );
            final String[] split = docId.split( ":" );
            final String type = split[ 0 ];

            Uri contentUri = null;
            if ( "image".equals( type ) ) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ( "video".equals( type ) ) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ( "audio".equals( type ) ) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[]{ split[ 1 ] };

            return getDataColumn( this, contentUri, selection,
                    selectionArgs );
        }
        else if ( "content".equalsIgnoreCase( uri.getScheme() ) ) {


            return getDataColumn( this, uri, null, null  );
        }
        // File
        else if ( "file".equalsIgnoreCase( uri.getScheme() ) ) {
            return uri.getPath();
        }

        return null;
    }


    public static String getDataColumn( Context context, Uri uri,
                                        String selection, String[] selectionArgs ) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query( uri, projection,
                    selection, selectionArgs, null );
            if ( cursor != null && cursor.moveToFirst() ) {
                final int column_index = cursor.getColumnIndexOrThrow( column );
                return cursor.getString( column_index );
            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_main, menu );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {

        super.onPrepareOptionsMenu( menu );

        if( mMode == FaceDetector.FAST_MODE )
            menu.findItem( R.id.fast ).setChecked( true );
        else
            menu.findItem( R.id.accurate ).setChecked( true );


        if( mLandmarks == FaceDetector.NO_LANDMARKS)
            menu.findItem( R.id.no_landmarks ).setChecked( true );
        else
            menu.findItem( R.id.all_landmarks  ).setChecked( true );

        if( mClassificationType == FaceDetector.NO_CLASSIFICATIONS )
            menu.findItem( R.id.no_classifications ).setChecked( true );
        else
            menu.findItem( R.id.all_classifications ).setChecked( true );


        switch( mThumbSize){

            case 360:
                menu.findItem( R.id.size360 ).setChecked(  true );
                break;
            case 640:
                menu.findItem( R.id.size640 ).setChecked(  true );
                break;
            case 720:
                menu.findItem( R.id.size720 ).setChecked(  true );
                break;

            case 1080:
                menu.findItem( R.id.size1080 ).setChecked( true  );
                break;

            case 2160:
                menu.findItem( R.id.size2160 ).setChecked( true  );
                break;
        }

        return true;

    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {

        boolean reloadDetector = false;

        switch ( item.getItemId() ) {

            case R.id.fast:

                mMode = FaceDetector.FAST_MODE;
                reloadDetector = true;
                break;

            case R.id.accurate:
                mMode = FaceDetector.ACCURATE_MODE;
                reloadDetector = true;
                break;

            case R.id.no_classifications:
                mClassificationType = FaceDetector.NO_CLASSIFICATIONS;
                reloadDetector = true;
                break;

            case R.id.all_classifications:
                mClassificationType = FaceDetector.ALL_CLASSIFICATIONS;
                reloadDetector = true;
                break;

            case R.id.all_landmarks:
                mLandmarks = FaceDetector.ALL_LANDMARKS;
                reloadDetector = true;
                break;

            case R.id.no_landmarks:
                mLandmarks = FaceDetector.NO_LANDMARKS;
                reloadDetector = true;
                break;


            case R.id.size360:
                mThumbSize = 360;
                break;

            case R.id.size640:
                mThumbSize = 640;
                break;

            case R.id.size720:
                mThumbSize = 720;
                break;

            case R.id.size1080:
                mThumbSize = 1080;
                break;

            case R.id.size2160:
                mThumbSize = 2160;
                break;

            default:
                return super.onOptionsItemSelected( item );
        }

        invalidateOptionsMenu();

        if( reloadDetector ){

          loadDetector();
        }

        return true;
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {

        if ( requestCode == RQS_LOADIMAGE && resultCode == RESULT_OK ) {

            if ( myBitmap != null ) {

                myBitmap.recycle();
            }

            mImagePath = getFileName( data.getData() );
            setImage();
        }

        super.onActivityResult( requestCode, resultCode, data );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    requestImage();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText( this, "Permission Read External Storage not granted :(", Toast.LENGTH_LONG ).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void setImage(){

        try {

            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
            SizeT screenSize = new SizeT( displayMetrics.widthPixels, displayMetrics.heightPixels );

            mImageSize = new SizeT();

            myBitmap = ImageUtils.decodeFile( this, mImagePath, screenSize, mImageSize );
            imgView.setImageBitmap( myBitmap );

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState, PersistableBundle outPersistentState ) {

        super.onSaveInstanceState( outState, outPersistentState );

        outState.putString( KEY_IMAGE_PATH, mImagePath );
        outState.putInt( KEY_IMAGE_SIZE_WIDTH, mImageSize != null ? mImageSize.width : 0 );
        outState.putInt( KEY_IMAGE_SIZE_HEIGHT, mImageSize != null ? mImageSize.height : 0 );

        outState.putInt( KEY_THUMB_SIZE, mThumbSize );
        outState.putInt( KEY_MODE , mMode);
        outState.putInt( KEY_LANDMARKS , mLandmarks );
        outState.putInt( KEY_CLASSIFICATION_TYPE, mClassificationType  );
    }


    private void detectFace() {

        long startTime = System.currentTimeMillis();

        StringBuilder stringBuilder = new StringBuilder();

        //Create a Paint object for drawing with
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth( 5 );
        myRectPaint.setColor( Color.GREEN );
        myRectPaint.setStyle( Paint.Style.STROKE );

        float ratio = ( float ) myBitmap.getHeight() / ( float ) myBitmap.getWidth();

        long startCreateBitmapTime = System.currentTimeMillis();
        //Create a Canvas object for drawing on
        Bitmap tempBitmap = Bitmap.createScaledBitmap( myBitmap, mThumbSize, ( int ) ( mThumbSize * ratio ), false );
        long endCreateBitmapTime = System.currentTimeMillis();

        Canvas tempCanvas = new Canvas( tempBitmap );

        if ( !faceDetector.isOperational() ) {

            Log.w( TAG, "Detector dependencies are not yet available." );

            IntentFilter lowstorageFilter = new IntentFilter( Intent.ACTION_DEVICE_STORAGE_LOW );
            boolean hasLowStorage = registerReceiver( null, lowstorageFilter ) != null;

            if ( hasLowStorage ) {

                Toast.makeText( this, "Low storage on device", Toast.LENGTH_LONG ).show();
                Log.w( TAG, "Low storage on device" );
            }
        }

        long startCreateFrameTime = System.currentTimeMillis();
        Frame frame = new Frame.Builder().setBitmap( tempBitmap ).build();
        long endCreateFrameTime = System.currentTimeMillis();

        stringBuilder.append( String.format( "Detector On: %s \n", faceDetector.isOperational() ? "YES" : "NO" ) );
        stringBuilder.append( String.format( "Detector Mode: %s \n", mMode == FaceDetector.FAST_MODE ? "Fast" : "Accurate" ) );
        stringBuilder.append( String.format( "Detector Classifications: %s \n", mClassificationType == FaceDetector.ALL_CLASSIFICATIONS ? "All" : "No" ) );
        stringBuilder.append( String.format( "Detector Landmarks: %s \n", mLandmarks == FaceDetector.ALL_LANDMARKS ? "All" : "No" ) );

        if ( mImageSize != null )
            stringBuilder.append( String.format( "Bitmap Size: %d x %d  \n", mImageSize.width, mImageSize.height ) );

        stringBuilder.append( String.format( "Thumb Size: %d x %d \n", tempBitmap.getWidth(), tempBitmap.getHeight() ) );

        long startDetectTime = System.currentTimeMillis();
        SparseArray< Face > faces = faceDetector.detect( frame );
        long endDetectTime = System.currentTimeMillis();


        stringBuilder.append( String.format( "Detected Faces: %d \n", faces.size() ) );

        //Draw Rectangles on the Faces
        for ( int i = 0; i < faces.size(); i++ ) {

            Face thisFace = faces.valueAt( i );
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect( new RectF( x1, y1, x2, y2 ), 2, 2, myRectPaint );

            if( mLandmarks == FaceDetector.ALL_LANDMARKS )
                for( Landmark landmark : thisFace.getLandmarks() )
                    tempCanvas.drawCircle( landmark.getPosition().x, landmark.getPosition().y, 10, myRectPaint );
        }

        imgView.setImageDrawable( new BitmapDrawable( getResources(), tempBitmap ) );

        long endTime = System.currentTimeMillis();

        stringBuilder.append( String.format( "Create Thumb Took %d ms \n", endCreateBitmapTime - startCreateBitmapTime ) );
        stringBuilder.append( String.format( "Create Frame Took %d ms \n", endCreateFrameTime - startCreateFrameTime ) );
        stringBuilder.append( String.format( "Detection Took %d ms \n", endDetectTime - startDetectTime ) );
        stringBuilder.append( String.format( "Total op. Took %d ms \n", endTime - startTime ) );

        textView.setText( stringBuilder.toString() );
    }


    @Override
    public void onStart() {

        super.onStart();


    }

    @Override
    public void onStop() {

        super.onStop();

    }
}
