package com.example.androidfacedetection;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Michail Lukov on 11/18/2016.
 */

public class ImageUtils {

    private static final String LOG_TAG = ImageUtils.class.getSimpleName();
    public static final double THUMBNAIL_MAX_SIZE_DOWN_SCALE = 1.2;
    public static final String SCHEME_FILE = "file://";
    public static final String EXIF_DATE_TIME_FORMAT = "yyyy:MM:dd hh:mm:ss";


    public static Bitmap decodeFile( Context context, final String imagePath, final SizeT requestedSize, SizeT imageSizeOut ) {

        final SizeT imageSize = getImageSize( context, imagePath );

        if( imageSizeOut != null )
            imageSizeOut.set( imageSize );

        float ratio = ( float ) Math.max( requestedSize.height, requestedSize.width )
                / ( float ) Math.max( imageSize.height, imageSize.width );

        SizeT thumbSize = new SizeT( ( int ) ( ( float ) imageSize.width * ratio ),
                ( int ) ( ( float ) imageSize.height * ratio ) );

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        if ( ( Math.max( requestedSize.width, requestedSize.height ) * 2 ) < Math.max( imageSize.width, imageSize.height ) ) {

            options.inSampleSize = calculateInSampleSize( imageSize, thumbSize );
        }

        final Bitmap thumbnail = BitmapFactory.decodeFile( imagePath, options );

        int orientation = readExifOrientation( context, Uri.parse( imagePath ) );

        return rotateBitmap( thumbnail, orientation );
    }


    public static Bitmap rotateBitmap( Bitmap bitmap, int orientation ) {

        Matrix matrix = createMatrixForExifOrientation( orientation );

        if ( matrix.isIdentity() )
            return bitmap;

        try {

            Bitmap bmRotated = Bitmap.createBitmap( bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true );
            bitmap.recycle();
            return bmRotated;
        } catch ( OutOfMemoryError e ) {
            e.printStackTrace();
            return null;
        }
    }


    public static Matrix createMatrixForExifOrientation( int exifOrientation ) {

        Matrix matrix = new Matrix();

        switch ( exifOrientation ) {
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale( -1, 1 );
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate( 180 );
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate( 180 );
                matrix.postScale( -1, 1 );
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate( 90 );
                matrix.postScale( -1, 1 );
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate( 90 );
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate( -90 );
                matrix.postScale( -1, 1 );
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate( -90 );
                break;
            default:
                break;
        }

        return matrix;
    }


    public static int readExifOrientation( Context context, Uri uri ) {

        if ( uri.getScheme() != null && uri.getScheme().equals( "content" ) ) {

            String[] projection = { MediaStore.Images.ImageColumns.ORIENTATION };

            String path = uri.getPath();
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection, MediaStore.Images.Media.DATA + "=?",
                        new String[]{ path }, null );
                if ( cursor.moveToFirst() ) {
                    return degreesToExifOrientation( cursor.getInt( 0 ) );
                }
            } finally {
                if ( cursor != null )
                    cursor.close();
            }

        } else {

            try {

                final ExifData exifData = readExifData( uri.getPath() );

                if ( exifData != null ) {

                    Log.i( "Photo_Rotation", "" + exifData.getExifRotation() );

                    return exifData.getExifRotation();
                }
            } catch ( Exception e ) {

                e.printStackTrace();
            }
        }

        return 0;
    }

    private static int degreesToExifOrientation( int degrees ) {

        if ( degrees == 0 ) {
            return ExifInterface.ORIENTATION_NORMAL;
        } else if ( degrees % 270 == 0 ) {
            return ExifInterface.ORIENTATION_ROTATE_270;
        } else if ( degrees % 180 == 0 ) {
            return ExifInterface.ORIENTATION_ROTATE_180;
        } else if ( degrees % 90 == 0 ) {
            return ExifInterface.ORIENTATION_ROTATE_90;
        }

        return ExifInterface.ORIENTATION_NORMAL;
    }

    public static int calculateInSampleSize( final SizeT imageSize, SizeT reqSize ) {

        // Raw height and width of image
        final int imageMaxDimen = Math.max( imageSize.width, imageSize.height );
        final int imageMaxReqDimen = Math.max( reqSize.width, reqSize.height );
        int inSampleSize = 1;

        if ( imageMaxDimen > imageMaxReqDimen ) {

            int maxDimen = imageMaxDimen;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ( ( maxDimen / inSampleSize ) >= ( imageMaxReqDimen / THUMBNAIL_MAX_SIZE_DOWN_SCALE ) ) {

                inSampleSize *= 2;
                maxDimen /= inSampleSize;
            }
        }

        return inSampleSize;
    }

    public static final SizeT getImageSize( final Context context, final String filePath ) {

        SizeT imageSize = null;

        try {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeFile( filePath, options );

            if ( options.outHeight > 0 && options.outWidth > 0 )
                imageSize = new SizeT( options.outWidth, options.outHeight );

        } catch ( Exception ex ) {

            Log.e( LOG_TAG, ex.getLocalizedMessage(), ex );
        }

        return imageSize;
    }

    public static final ExifData readExifData( String localPath ) throws Exception {

        ExifData fileExifData = null;
        File imageFile = new File( localPath );

        if ( imageFile.exists() == false )
            imageFile = new File( localPath.replace( ImageUtils.SCHEME_FILE, "" ) );

        if ( imageFile.exists() ) {

            Date imageDate = null;
            SizeT imageSize = null;
            ExifInterface exif = new ExifInterface( imageFile.getAbsolutePath() );

            String exifDate = exif.getAttribute( ExifInterface.TAG_DATETIME );
            int rotation = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL );
            int width = exif.getAttributeInt( ExifInterface.TAG_IMAGE_WIDTH, -1 );
            int height = exif.getAttributeInt( ExifInterface.TAG_IMAGE_LENGTH, -1 );

            if ( width > 0 && height > 0 )
                imageSize = new SizeT( width, height );
            //else
            //    imageSize = getImageSize( localPath );

            if ( TextUtils.isEmpty( exifDate ) == false )
                imageDate = new SimpleDateFormat( EXIF_DATE_TIME_FORMAT ).parse( exifDate );

            fileExifData = new ExifData( rotation, imageSize, imageDate, localPath );
        }

        return fileExifData;
    }
}
