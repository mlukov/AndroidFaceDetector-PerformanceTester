package com.example.androidfacedetection;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michail Lukov on 11/18/2016.
 */


public class SizeT implements Serializable {

    public int width = 0;
    public int height = 0;

    public SizeT() {

    }

    public SizeT( final SizeT size ) {

        this.width = size.width;
        this.height = size.height;
    }

    public SizeT( int width, int height ) {

        this.width = width;
        this.height = height;
    }

    /**
     * Return scale factor to fit in SizeT object
     *
     * @param sizeT SizeT object
     * @return scale factor
     */
    public float scaleToFitInSize( final SizeT sizeT ) {

        return Math.min( ( float ) sizeT.width / ( float ) width, ( float ) sizeT.height / ( float ) height );
    }

    public float scaleToFillSize( final SizeT sizeT ) {

        return Math.max( ( float ) sizeT.width / ( float ) width, ( float ) sizeT.height / ( float ) height );
    }

    public void set( int width, int height ) {

        this.width = width;
        this.height = height;
    }

    public void set( SizeT sizeT ) {

        this.width = sizeT.width;
        this.height = sizeT.height;
    }

    /**
     * Creates a scaled  SizeT object based on the current object
     *
     * @param scaleFactor factor for inflation
     * @return new instance of scaled SizeT
     */
    public SizeT scale( final double scaleFactor ) {

        return new SizeT( ( int ) Math.floor( ( float ) width * scaleFactor )
                , ( int ) Math.floor( ( float ) height * scaleFactor ) );
    }

    /*
    * Compare SizeT objects and returns true if theirs height and width are equal
    * @param sizeT SizeT object
    * @return boolean
    */
    public boolean isSame( SizeT sizeT ) {

        boolean isEqual = false;
        if ( sizeT != null ) {

            isEqual = this.width == sizeT.width && this.height == sizeT.height;
        }
        return isEqual;
    }

    /**
     * Return a string representation of the size
     * <p>You can later recover the SizeT from this string through
     * {@link #unflattenFromString(String)}.
     *
     * @return Returns a new String of the form "Width,Height"
     */
    public String flattenToString() {

        StringBuilder sb = new StringBuilder( 32 );
        sb.append( width );
        sb.append( ',' );
        sb.append( height );
        return sb.toString();
    }

    public final boolean isSmallerThanSize( final SizeT sizeT ) {

        return width < sizeT.width && height < sizeT.height;
    }

    /**
     * Returns a SizeT from a string of the form returned by {@link #flattenToString},
     * or null if the string is not of that form.
     *
     * @param str - string generated from {@link #flattenToString()} method
     * @return SizeT
     */
    public static SizeT unflattenFromString( String str ) {

        Matcher matcher = UnflattenHelper.getMatcher( str );
        if ( !matcher.matches() ) {
            return null;
        }
        return new SizeT( Integer.parseInt( matcher.group( 1 ) ),
                Integer.parseInt( matcher.group( 2 ) ) );
    }

    private static final class UnflattenHelper {

        private static final Pattern FLATTENED_PATTERN = Pattern.compile(
                "(-?\\d+),(-?\\d+)" );

        static Matcher getMatcher( String str ) {

            return FLATTENED_PATTERN.matcher( str );
        }
    }
}

