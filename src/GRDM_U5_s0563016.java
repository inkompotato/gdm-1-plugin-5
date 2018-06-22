import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.*;

/**
 Opens an image window and adds a panel below the image
 */
public class GRDM_U5_s0563016 implements PlugIn {

    ImagePlus imp; // ImagePlus object
    private int[] origPixels;
    private int width;
    private int height;

    String[] items = {"Original", "Weichzeichnen", "Hochpass", "Kanten", "Kanten2"};


    public static void main(String args[]) {


        IJ.open("C:\\Users\\frajs\\Creative Cloud Files\\Eclipse\\workspace-gdm1\\gdm-1-plugin-5\\src\\sail.jpg");
        //IJ.open("Z:/Pictures/Beispielbilder/orchid.jpg");

        GRDM_U5_s0563016 pw = new GRDM_U5_s0563016();
        pw.imp = IJ.getImage();
        pw.run("");
    }

    public void run(String arg) {
        if (imp==null)
            imp = WindowManager.getCurrentImage();
        if (imp==null) {
            return;
        }
        CustomCanvas cc = new CustomCanvas(imp);

        storePixelValues(imp.getProcessor());

        new CustomWindow(imp, cc);
    }


    private void storePixelValues(ImageProcessor ip) {
        width = ip.getWidth();
        height = ip.getHeight();

        origPixels = ((int []) ip.getPixels()).clone();
    }


    class CustomCanvas extends ImageCanvas {

        CustomCanvas(ImagePlus imp) {
            super(imp);
        }

    } // CustomCanvas inner class


    class CustomWindow extends ImageWindow implements ItemListener {

        private String method;

        CustomWindow(ImagePlus imp, ImageCanvas ic) {
            super(imp, ic);
            addPanel();
        }

        void addPanel() {
            //JPanel panel = new JPanel();
            Panel panel = new Panel();

            JComboBox cb = new JComboBox(items);
            panel.add(cb);
            cb.addItemListener(this);

            add(panel);
            pack();
        }

        public void itemStateChanged(ItemEvent evt) {

            // Get the affected item
            Object item = evt.getItem();

            if (evt.getStateChange() == ItemEvent.SELECTED) {
                System.out.println("Selected: " + item.toString());
                method = item.toString();
                changePixelValues(imp.getProcessor());
                imp.updateAndDraw();
            }

        }


        private void changePixelValues(ImageProcessor ip) {

            // Array zum ZurÃ¼ckschreiben der Pixelwerte
            int[] pixels = (int[])ip.getPixels();

            if (method.equals("Original")) {

                for (int y=0; y<height; y++) {
                    for (int x=0; x<width; x++) {
                        int pos = y*width + x;

                        pixels[pos] = origPixels[pos];
                    }
                }
            }

            if (method.equals("Weichzeichnen")) {

                blur(pixels);
            }

            if (method.equals("Hochpass")) {
                highpass(pixels);
            }

            if (method.equals("Kanten")){
                sharpen(pixels);
            }

            if (method.equals("Kanten2")){
                blur(pixels);
                sharpen(pixels);
            }

        }

        private void highpass(int[] pixels) {
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int pos = y*width + x;
                    int[] positions = {pos-1, pos+1, pos+width-1, pos+width, pos+width+1, pos-width-1, pos-width, pos-width+1};
                    int argb = origPixels[pos];  // Lesen der Originalwerte

                    float r=0,g=0,b=0;

                    for (int position : positions) {

                        if (position > 0 && position < height * width) {

                            r += ((origPixels[position] >> 16) & 0xff) / (-9F);
                            g += ((origPixels[position] >> 8) & 0xff) / (-9F);
                            b += ((origPixels[position]) & 0xff) / (-9F);
                        } else {
                            r += ((origPixels[pos] >> 16) & 0xff) / 9;
                            g += ((origPixels[pos] >> 8) & 0xff) / 9;
                            b += ((origPixels[pos]) & 0xff) / 9;
                        }
                    }

                    r += ((origPixels[pos] >> 16) & 0xff)*(8F/9F);
                    g += ((origPixels[pos] >> 8) & 0xff)*(8F/9F);
                    b += ((origPixels[pos]) & 0xff)*(8F/9F);

                    int rn = preventOverflow(Math.round(r+128));
                    int gn = preventOverflow(Math.round(g+128));
                    int bn = preventOverflow(Math.round(b+128));


                    pixels[pos] = (0xFF<<24) | (rn<<16) | (gn << 8) | bn;
                }
            }
        }

        private void sharpen(int[] pixels) {
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int pos = y*width + x;
                    int[] positions = {pos-1, pos+1, pos+width-1, pos+width, pos+width+1, pos-width-1, pos-width, pos-width+1};
                    int argb = origPixels[pos];  // Lesen der Originalwerte

                    float r=0,g=0,b=0;

                    for (int position : positions) {

                        if (position > 0 && position < height * width) {

                            r += ((origPixels[position] >> 16) & 0xff) / -9F;
                            g += ((origPixels[position] >> 8) & 0xff) / -9F;
                            b += ((origPixels[position]) & 0xff) / -9F;
                        } else {
                            r += ((origPixels[pos] >> 16) & 0xff) / 9;
                            g += ((origPixels[pos] >> 8) & 0xff) / 9;
                            b += ((origPixels[pos]) & 0xff) / 9;
                        }
                    }

                    r += ((origPixels[pos] >> 16) & 0xff)*(17F/9F);
                    g += ((origPixels[pos] >> 8) & 0xff)*(17F/9F);
                    b += ((origPixels[pos]) & 0xff)*(17F/9F);

                    int rn = preventOverflow(Math.round(r));
                    int gn = preventOverflow(Math.round(g));
                    int bn = preventOverflow(Math.round(b));


                    pixels[pos] = (0xFF<<24) | (rn<<16) | (gn << 8) | bn;
                }
            }
        }

        private void blur(int[] pixels) {
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int pos = y*width + x;
                    int[] positions = {pos-1, pos, pos+1, pos+width-1, pos+width, pos+width+1, pos-width-1, pos-width, pos-width+1};
                    int argb = origPixels[pos];  // Lesen der Originalwerte


                    int r = 0;
                    int g = 0;
                    int b = 0;

                    for (int position : positions) {
                        if (position > 0 && position < height * width) {
                            r += ((origPixels[position] >> 16) & 0xff) / 9;
                            g += ((origPixels[position] >> 8) & 0xff) / 9;
                            b += ((origPixels[position]) & 0xff) / 9;
                        } else {
                            r += ((origPixels[pos] >> 16) & 0xff) / 9;
                            g += ((origPixels[pos] >> 8) & 0xff) / 9;
                            b += ((origPixels[pos]) & 0xff) / 9;
                        }
                    }


                    pixels[pos] = (0xFF<<24) | (r<<16) | (g << 8) | b;
                }
            }
        }

        private int preventOverflow(int c) {
            if (c > 255) c = 255;
            if (c < 0) c = 0;
            return c;
        }


    } // CustomWindow inner class
} 