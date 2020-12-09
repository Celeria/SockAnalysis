package edu.vanderbiltBME389002.sockanalysis;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    Bitmap sockImage;
    Bitmap sockImageSmaller;
    final int SCALE = 3;
    int newHeight;
    int newWidth;
    Bitmap afterCrop;
    Bitmap afterThreshold;
    ImageView initialImage;
    ImageView croppedImage;
    Button btnPickImage;
    Button check;
    Button btnTopLeft;
    Button btnBottomRight;
    Button btnCreateCrop;
    Button btnMoveUp;
    Button btnMoveDown;
    Button btnMoveLeft;
    Button btnMoveRight;
    Button btnFindCenter;
    Button btnCalculateStretch;
    TextView txtPercentageWhite;
    EditText editThreshold;
    GraphView gvStretch;

    int touchX = 9435000;
    int touchY = 9435000;

    int x1 = -1;
    int y1 = -1;
    int x2 = -1;
    int y2 = -1;

    boolean useTop = true;

    final int RESULT_LOAD_IMG = 103;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //regionAssign components
        initialImage = findViewById(R.id.initialImage);
        croppedImage = findViewById(R.id.croppedImage);
        check = findViewById(R.id.btnCheckColor);
        txtPercentageWhite = findViewById(R.id.txtPercentageWhite);
        editThreshold = findViewById(R.id.editThreshold);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnBottomRight = findViewById(R.id.btnBottomRight);
        btnTopLeft = findViewById(R.id.btnTopLeft);
        btnCreateCrop = findViewById(R.id.btnCreateCrop);
        btnMoveDown = findViewById(R.id.btnMoveDown);
        btnMoveLeft = findViewById(R.id.btnMoveLeft);
        btnMoveRight = findViewById(R.id.btnMoveRight);
        btnMoveUp = findViewById(R.id.btnMoveUp);
        btnFindCenter = findViewById(R.id.btnFindCenter);
        btnCalculateStretch = findViewById(R.id.btnCalculateStretch);
        gvStretch = findViewById(R.id.gvStretch);
        //endregion

        //Make it possible to touch the image to select
        initialImage.setOnTouchListener(this);

        //regionSelect an image to use
        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });
        //endregion

        //regionMove the selection area a little bit
        final int MOVE_AMOUNT = 2;
        btnMoveDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(touchY + MOVE_AMOUNT < newHeight) {
                    touchY += MOVE_AMOUNT;
                } else {
                    Toast.makeText(getApplicationContext(),"Reached End.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMoveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(touchY - MOVE_AMOUNT > 0) {
                    touchY -= MOVE_AMOUNT;
                } else {
                    Toast.makeText(getApplicationContext(),"Reached End.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMoveRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(touchX + MOVE_AMOUNT < newWidth) {
                    touchX += MOVE_AMOUNT;
                } else {
                    Toast.makeText(getApplicationContext(),"Reached End.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMoveLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(touchX - MOVE_AMOUNT > 0) {
                    touchX -= MOVE_AMOUNT;
                } else {
                    Toast.makeText(getApplicationContext(),"Reached End.",Toast.LENGTH_SHORT).show();
                }
            }
        });
        //endregion

        //region Display what the crop will look like
            //Place to store first bitmap
            final Bitmap[] topCornerBitmap = new Bitmap[1];
            final Toast noAbove = Toast.makeText(getApplicationContext(),"Can't put bottom corner above top corner.\n" +
                    "Press CREATE TOP CORNER to reset location if desired",Toast.LENGTH_LONG);
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    boolean changed = true;
                    int prevX = 0;
                    int prevY = 0;
                    while (true) {
                        //Perform this if the value touched has changed
                        try {
                            if (changed && touchX != 9435000) {
                                changed = false;
                                //Locate the top left corner
                                if (useTop) {
                                    Bitmap linedBitmap = sockImageSmaller.copy(Bitmap.Config.ARGB_8888, true);
                                    int h = linedBitmap.getHeight();
                                    int w = linedBitmap.getWidth();
                                    //Create vertical red line
                                    for (int i = 0; i < h; ++i) {
                                        //This part is so it doesn't crash if you somehow manage to hit the very edge of the picture
                                        try {
                                            linedBitmap.setPixel(touchX - 1, i, Color.BLACK);
                                            linedBitmap.setPixel(touchX + 1, i, Color.BLACK);
                                        } catch (Exception e) {
                                            //Do nothing
                                        }
                                        linedBitmap.setPixel(touchX, i, Color.WHITE);
                                    }
                                    //Create horizontal red line
                                    for (int i = 0; i < w; ++i) {
                                        try {
                                            linedBitmap.setPixel(i, touchY - 1, Color.BLACK);
                                            linedBitmap.setPixel(i, touchY + 1, Color.BLACK);
                                        } catch (Exception e) {
                                            //Do nothing
                                        }
                                        linedBitmap.setPixel(i, touchY, Color.WHITE);
                                    }
                                    initialImage.setImageBitmap(linedBitmap);
                                    topCornerBitmap[0] = linedBitmap;
                                    x1 = touchX * SCALE;
                                    y1 = touchY * SCALE;
                                } else {
                                    //Make sure bottom right isn't above top left
                                    if ((touchX * SCALE) < x1 || (touchY * SCALE) < y1) {
                                        noAbove.show();
                                    } else {
                                        //Locate the bottom right corner
                                        Bitmap linedBitmap2 = topCornerBitmap[0].copy(Bitmap.Config.ARGB_8888, true);
                                        int h = linedBitmap2.getHeight();
                                        int w = linedBitmap2.getWidth();
                                        //Create vertical blue line
                                        for (int i = 0; i < h; ++i) {
                                            //This part is so it doesn't crash if you somehow manage to hit the very edge of the picture
                                            try {
                                                linedBitmap2.setPixel(touchX - 1, i, Color.BLACK);
                                                linedBitmap2.setPixel(touchX + 1, i, Color.BLACK);
                                            } catch (Exception e) {
                                                //Do nothing
                                            }
                                            linedBitmap2.setPixel(touchX, i, Color.WHITE);
                                        }
                                        //Create horizontal blue line
                                        for (int i = 0; i < w; ++i) {
                                            try {
                                                linedBitmap2.setPixel(i, touchY - 1, Color.BLACK);
                                                linedBitmap2.setPixel(i, touchY + 1, Color.BLACK);
                                            } catch (Exception e) {
                                                //Do nothing
                                            }
                                            linedBitmap2.setPixel(i, touchY, Color.WHITE);
                                        }
                                        initialImage.setImageBitmap(linedBitmap2);
                                        x2 = touchX * SCALE;
                                        y2 = touchY * SCALE;
                                    }
                                }
                            } else {
                                //Check if a change has happened, run this easier thread instead to save resources.
                                if (prevX != touchX || prevY != touchY) {
                                    changed = true;
                                    prevX = touchX;
                                    prevY = touchY;
                                }
                                try {
                                    Thread.sleep(3);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            //Do nothing, this just prevents any silly crashes if somehow you tap outside the bounds
                        }
                    }
                }
            }.start();
        //endregion

        //regionSelect which corner to use
        btnTopLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                useTop = true;
            }
        });

        btnBottomRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                useTop = false;
            }
        });
        //endregion

        //regionCreate the cropped image to display
        btnCreateCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //First make sure its able to be done
                if(x1 == -1 || x2 == -1){
                    Toast.makeText(getApplicationContext(),"Select corners first.",Toast.LENGTH_SHORT).show();
                } else {
                    Bitmap croppedImageBit = Bitmap.createBitmap(sockImage,x1,y1,Math.abs(x1-x2),Math.abs(y1-y2));
                    afterCrop = croppedImageBit;
                    Bitmap tempImage = Bitmap.createScaledBitmap(afterCrop,croppedImage.getWidth(),croppedImage.getHeight(),false);
                    croppedImage.setImageBitmap(tempImage);
                }
            }
        });
        //endregion

        //regionCheck how well the threshold worked
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int threshold;
                try {
                    threshold = Integer.parseInt(editThreshold.getText().toString());
                    int height,width;
                    final Bitmap editedImage = Bitmap.createBitmap(afterCrop);
                    height = editedImage.getHeight();
                    width = editedImage.getWidth();
                    int[] pixels = new int[height*width];
                    editedImage.getPixels(pixels,0,width,0,0,width,height);
                    int total = 0;
                    int whitePixels = 0;
                    int w = 0;
                    for (int counter = 0; counter < height*width; ++counter) {
                        int red, blue, green;
                        red = Color.red(pixels[counter]);
                        blue = Color.blue(pixels[counter]);
                        green = Color.green(pixels[counter]);
                        if ((red + blue + green) < threshold) {
                            //set to black
                            pixels[counter] = Color.BLACK;
                        } else {
                            //set to white
                            pixels[counter] = Color.WHITE;
                            ++whitePixels;
                        }
                        ++total;
                        ++w;
                        if (w % width == 0) {
                            w = 0;
                        }
                    }
                    Bitmap editedBitmap2 = Bitmap.createBitmap(pixels,width,height, Bitmap.Config.ARGB_8888);
                    afterThreshold = editedBitmap2;
                    Bitmap smallerBitmap = editedBitmap2.createScaledBitmap(editedBitmap2,initialImage.getWidth(),initialImage.getHeight(),false);
                    croppedImage.setImageBitmap(smallerBitmap);
                    String display = "The image is " + Round((100 * (double)whitePixels / (double)total)) + "% white.";
                    txtPercentageWhite.setText(display);
                }catch(Exception e){
                    Toast.makeText(getApplicationContext(),"Enter a number between 0 and 765",Toast.LENGTH_LONG).show();
                }
            }
        });
        //endregion

        //regionExtract center information
        final ArrayList<Integer>[] centerInfo = new ArrayList[]{new ArrayList<>()};
        final ArrayList<Integer>[] widthInfo = new ArrayList[]{new ArrayList<>()};
        btnFindCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int height = afterThreshold.getHeight();
                int width = afterThreshold.getWidth();
                int[] pixels = new int[height*width];
                int[] fixedPixels = new int[height*width];
                int counter = 0;
                centerInfo[0] = new ArrayList<>();
                afterThreshold.getPixels(pixels,0,width,0,0,width,height);
                for(int h = 0; h < height; ++h){
                    int start = width*h;
                    int end = (width*(h+1));
                    for (int p = width*h; p < (width*(h+1));++p) {
                        if (pixels[p] == Color.BLACK) {
                            start = p;
                            break;
                        }
                    }
                    for (int p = (width*(h+1))-1; p > width*h; --p){
                        if(pixels[p] == Color.BLACK) {
                            end = p;
                            break;
                        }
                    }
                    boolean reachedStart = false;
                    boolean reachedEnd = false;
                    for (int w = 0; w < width; ++w){
                        if(!reachedStart) {
                            if (counter < start) {
                                fixedPixels[counter] = Color.WHITE;
                            } else {
                                reachedStart = true;
                                fixedPixels[counter] = Color.BLACK;
                            }
                            ++counter;
                        } else if (!reachedEnd) {
                            if (counter < end) {
                                fixedPixels[counter] = Color.BLACK;
                            } else {
                                reachedEnd = true;
                                fixedPixels[counter] = Color.WHITE;
                            }
                            ++counter;
                        } else {
                            fixedPixels[counter] = Color.WHITE;
                            counter++;
                        }
                    }
                    //Set middle to white so you can see the middle
                    int middle = ((end - start)/2) + start;
                    centerInfo[0].add(middle);
                    widthInfo[0].add(end-start);
                    fixedPixels[middle] = Color.WHITE;
                    fixedPixels[middle + 1] = Color.WHITE;
                    fixedPixels[middle - 1] = Color.WHITE;
                }
                Bitmap editedBitmap = Bitmap.createBitmap(fixedPixels,width,height, Bitmap.Config.ARGB_8888);
                Bitmap smallerBitmap = editedBitmap.createScaledBitmap(editedBitmap,initialImage.getWidth(),initialImage.getHeight(),false);
                croppedImage.setImageBitmap(smallerBitmap);
            }
        });
        //endregion

        //regionGraph the stretch
        btnCalculateStretch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int height = afterCrop.getHeight();
                int width = afterCrop.getWidth();
                int[] pixels = new int[height*width];
                afterCrop.getPixels(pixels,0,width,0,0,width,height);
                LineGraphSeries<DataPoint> stretchAmounts = new LineGraphSeries<>();
                int xCounter = 0;

                //Go through the list of middle points in centerInfo
                int SIDEWAYS = 200;
                int stretchAmount = 0;
                int rowsToCombine = 70;
                int rowCount = 0;
                for(int center:centerInfo[0]){
                    for(int i = center - SIDEWAYS; i < center + SIDEWAYS; ++i) {
                        int currentColor = pixels[i];
                        int amountRed = Color.red(currentColor);
                        int amountBlue = Color.blue(currentColor);
                        int amountGreen = Color.green(currentColor);
                        stretchAmount += amountBlue + amountRed + amountGreen;
                    }
                    ++rowCount;
                    if (rowCount > rowsToCombine) {
                        stretchAmounts.appendData(new DataPoint(xCounter,stretchAmount),false,centerInfo[0].size(),true);
                        ++xCounter;
                        rowCount = 0;
                        stretchAmount = 0;
                    }

                }
                gvStretch.removeAllSeries();
                gvStretch.addSeries(stretchAmounts);

                int height2 = afterCrop.getHeight();
                int width2 = afterCrop.getWidth();
                int[] pixels2 = new int[height2*width2];
                afterThreshold.getPixels(pixels2,0,width2,0,0,width2,height2);
                rowCount = 0;
                int showUp = 0;
                for(int center:centerInfo[0]){
                    for(int i = center - SIDEWAYS; i < center + SIDEWAYS; ++i) {
                        if (rowCount > rowsToCombine) {
                            rowCount = 0;
                            showUp = 5;
                        } else {
                            if (showUp > 0) {
                                pixels2[i] = Color.BLUE;
                            } else {
                                pixels2[i] = Color.GREEN;
                            }
                        }
                    }
                    if (showUp > 0) {
                        --showUp;
                    }
                    ++rowCount;
                }

                Bitmap editedBitmap = Bitmap.createBitmap(pixels2,width,height, Bitmap.Config.ARGB_8888);
                Bitmap smallerBitmap = editedBitmap.createScaledBitmap(editedBitmap,initialImage.getWidth(),initialImage.getHeight(),false);
                croppedImage.setImageBitmap(smallerBitmap);
            }
        });
        //endregion
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                sockImage = BitmapFactory.decodeStream(imageStream);
                //regionShrink image down a little so the system can handle it
                newHeight = sockImage.getHeight()/SCALE;
                newWidth = sockImage.getWidth()/SCALE;
                sockImageSmaller = sockImage.createScaledBitmap(sockImage,newWidth,newHeight,false);
                initialImage.setImageBitmap(sockImageSmaller);
                //endregion

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(getApplicationContext(), "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }


    //Quick method for removing pesky floating point imprecision decimals
    public double Round(double input) {
        BigDecimal bd = BigDecimal.valueOf(input);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if(view.getId() == R.id.initialImage) {
            touchX = (int)event.getX();
            touchY = (int)event.getY();
            Log.d("TOUCHING","Point tapped: " + (int)event.getX() + " " + (int)event.getY());
            return false;
        } else {
            return true;
        }
    }
}