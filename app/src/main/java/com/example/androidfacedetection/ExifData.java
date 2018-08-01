package com.example.androidfacedetection;

import java.util.Date;

/**
 * Created by Michail Lukov on 11/18/2016.
 */

public class ExifData {

    private final Date exifLastModifiedDate;
    private final int exifRotation;
    private final SizeT exifSize;
    private final String filePath;

    public ExifData( int rotation, SizeT size, Date exifDate, String file ) {

        filePath = file;
        exifSize = size;
        exifRotation = rotation;
        exifLastModifiedDate = exifDate;
    }

    public final Date getExifLastModifiedDate() {

        return exifLastModifiedDate;
    }

    public final int getExifRotation() {

        return exifRotation;
    }

    public final SizeT getExifSize() {

        return exifSize;
    }

    public final String getFilePath() {

        return filePath;
    }
}
