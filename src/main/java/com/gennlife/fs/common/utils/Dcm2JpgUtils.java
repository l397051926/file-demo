//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


package com.gennlife.fs.common.utils;

import org.apache.commons.cli.*;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.imageio.plugins.dcm.DicomImageReadParam;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.CloseUtils;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 * 反编译的代码
 */
public class Dcm2JpgUtils {
    private static final String USAGE = "dcm2jpg [Options] <dcmfile> <jpegfile>\nor dcm2jpg [Options] <dcmfile>... <outdir>\nor dcm2jpg [Options] <indir>... <outdir>";
    private static final String DESCRIPTION = "Convert DICOM image(s) to JPEG(s)\nOptions:";
    private static final String EXAMPLE = null;
    private int frame = 1;
    private float center;
    private float width;
    private String vlutFct;
    private boolean autoWindowing;
    private DicomObject prState;
    private short[] pval2gray;
    private String formatName = "JPEG";
    private String compressionType = "jpeg";
    private String fileExt = ".jpg";
    private Float imageQuality;
    private String imageWriterClassname = "*";

    public Dcm2JpgUtils() {
    }

    private void setFrameNumber(int frame) {
        this.frame = frame;
    }

    private void setWindowCenter(float center) {
        this.center = center;
    }

    private void setWindowWidth(float width) {
        this.width = width;
    }

    public final void setVoiLutFunction(String vlutFct) {
        this.vlutFct = vlutFct;
    }

    private final void setAutoWindowing(boolean autoWindowing) {
        this.autoWindowing = autoWindowing;
    }

    private final void setPresentationState(DicomObject prState) {
        this.prState = prState;
    }

    private final void setPValue2Gray(short[] pval2gray) {
        this.pval2gray = pval2gray;
    }

    public final void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    private void setImageWriter(String imagewriter) {
        this.imageWriterClassname = imagewriter;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    private void setImageQuality(int quality) {
        this.imageQuality = new Float((float)quality / 100.0F);
    }

    private ImageWriter getImageWriter(String imageWriterClass) throws IIOException {
        Iterator it = ImageIO.getImageWritersByFormatName(this.formatName);

        ImageWriter writer;
        do {
            if(!it.hasNext()) {
                throw new IIOException("No such ImageWriter - " + imageWriterClass);
            }

            writer = (ImageWriter)it.next();
        } while(!"*".equals(imageWriterClass) && !writer.getClass().getName().equals(imageWriterClass));

        return writer;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public void convert(File src, File dest) throws IOException {
        Iterator iter = ImageIO.getImageReadersByFormatName("DICOM");
        ImageReader reader = (ImageReader)iter.next();
        DicomImageReadParam param = (DicomImageReadParam)reader.getDefaultReadParam();
        param.setWindowCenter(this.center);
        param.setWindowWidth(this.width);
        param.setVoiLutFunction(this.vlutFct);
        param.setPresentationState(this.prState);
        param.setPValue2Gray(this.pval2gray);
        param.setAutoWindowing(this.autoWindowing);
        ImageInputStream iis = ImageIO.createImageInputStream(src);

        label44: {
            try {
                reader.setInput(iis, false);
                BufferedImage bi = reader.read(this.frame - 1, param);
                if(bi != null) {
                    this.encodeByImageIO(bi, dest);
                    break label44;
                }

                System.out.println("\nError: " + src + " - couldn\'t read!");
            } finally {
                CloseUtils.safeClose(iis);
            }

            return;
        }

        System.out.print('.');
    }

    private void encodeByImageIO(BufferedImage bi, File dest) throws IOException {
        ImageWriter writer = this.getImageWriter(this.imageWriterClassname);
        ImageOutputStream out = null;

        try {
            out = ImageIO.createImageOutputStream(dest);
            writer.setOutput(out);
            ImageWriteParam iwparam = writer.getDefaultWriteParam();
            if(!iwparam.canWriteCompressed()) {
                if(this.imageQuality != null) {
                    System.out.println("Selected Image Writer can not compress! imageQuality is ignored!");
                }
            } else {
                iwparam.setCompressionMode(2);
                String[] compressionTypes = iwparam.getCompressionTypes();
                if(compressionTypes != null && compressionTypes.length > 0 && (this.compressionType != null || iwparam.getCompressionType() == null)) {
                    for(int i = 0; i < compressionTypes.length; ++i) {
                        if(this.compressionType == null || compressionTypes[i].compareToIgnoreCase(this.compressionType) == 0) {
                            iwparam.setCompressionType(compressionTypes[i]);
                            break;
                        }
                    }
                }

                if(this.imageQuality != null) {
                    iwparam.setCompressionQuality(this.imageQuality.floatValue());
                }
            }

            writer.write((IIOMetadata)null, new IIOImage(bi, (List)null, (IIOMetadata)null), iwparam);
        } finally {
            CloseUtils.safeClose(out);
            writer.dispose();
        }

    }

    public int mconvert(List<String> args, int optind, File destDir) throws IOException {
        int count = 0;
        int i = optind;

        for(int n = args.size() - 1; i < n; ++i) {
            File src = new File((String)args.get(i));
            count += this.mconvert(src, new File(destDir, this.src2dest(src)));
        }

        return count;
    }

    private String src2dest(File src) {
        String srcname = src.getName();
        return src.isFile()?srcname + this.fileExt:srcname;
    }

    public int mconvert(File src, File dest) throws IOException {
        if(!src.exists()) {
            System.err.println("WARNING: No such file or directory: " + src + " - skipped.");
            return 0;
        } else if(src.isFile()) {
            try {
                this.convert(src, dest);
            } catch (Exception var6) {
                System.err.println("WARNING: Failed to convert " + src + ":");
                var6.printStackTrace(System.err);
                System.out.print('F');
                return 0;
            }

            System.out.print('.');
            return 1;
        } else {
            File[] files = src.listFiles();
            if(files.length > 0 && !dest.exists()) {
                dest.mkdirs();
            }

            int count = 0;

            for(int i = 0; i < files.length; ++i) {
                count += this.mconvert(files[i], new File(dest, this.src2dest(files[i])));
            }

            return count;
        }
    }

 /*   public static void main(String[] args) throws Exception {
        CommandLine cl = parse(args);
        Dcm2JpgUtils dcm2JpgUtils = new Dcm2JpgUtils();
        if(cl.hasOption("f")) {
            dcm2JpgUtils.setFrameNumber(parseInt(cl.getOptionValue("f"), "illegal argument of option -f", 1, 2147483647));
        }

        if(cl.hasOption("p")) {
            dcm2JpgUtils.setPresentationState(loadDicomObject(new File(cl.getOptionValue("p"))));
        }

        if(cl.hasOption("pv2gray")) {
            dcm2JpgUtils.setPValue2Gray(loadPVal2Gray(new File(cl.getOptionValue("pv2gray"))));
        }

        if(cl.hasOption("c")) {
            dcm2JpgUtils.setWindowCenter(parseFloat(cl.getOptionValue("c"), "illegal argument of option -c"));
        }

        if(cl.hasOption("w")) {
            dcm2JpgUtils.setWindowWidth(parseFloat(cl.getOptionValue("w"), "illegal argument of option -w"));
        }

        if(cl.hasOption("q")) {
            dcm2JpgUtils.setImageQuality(parseInt(cl.getOptionValue("q"), "illegal argument of option -q", 0, 100));
        }

        String argList;
        if(cl.hasOption("F")) {
            argList = cl.getOptionValue("F");
            dcm2JpgUtils.setFormatName(argList.toUpperCase());
            dcm2JpgUtils.setFileExt("." + argList.toLowerCase());
            dcm2JpgUtils.setCompressionType("JPEG".equalsIgnoreCase(argList)?"jpeg":null);
        }

        if(cl.hasOption("T")) {
            argList = cl.getOptionValue("T");
            dcm2JpgUtils.setCompressionType("*".equals(argList)?null:argList);
        }

        if(cl.hasOption("imagewriter")) {
            dcm2JpgUtils.setImageWriter(cl.getOptionValue("imagewriter"));
        }

        if(cl.hasOption("sigmoid")) {
            dcm2JpgUtils.setVoiLutFunction("SIGMOID");
        }

        dcm2JpgUtils.setAutoWindowing(!cl.hasOption("noauto"));
        if(cl.hasOption("jpgext")) {
            dcm2JpgUtils.setFileExt(cl.getOptionValue("jpgext"));
        }

        if(cl.hasOption("S")) {
            dcm2JpgUtils.showFormatNames();
        } else if(cl.hasOption("s")) {
            dcm2JpgUtils.showImageWriters();
        } else {
            List argList1 = cl.getArgList();
            int argc = argList1.size();
            File dest = new File((String)argList1.get(argc - 1));
            long t1 = System.currentTimeMillis();
            int count = 1;
            if(dest.isDirectory()) {
                count = dcm2JpgUtils.mconvert(argList1, 0, dest);
            } else {
                File t2 = new File((String)argList1.get(0));
                if(argc > 2 || t2.isDirectory()) {
                    exit("dcm2JpgUtils: when converting several files, last argument must be a directory\n");
                }

                if(!t2.exists()) {
                    exit("Cannot find the file specified: " + (String)argList1.get(0));
                }

                dcm2JpgUtils.convert(t2, dest);
            }

            long t21 = System.currentTimeMillis();
            System.out.println("\nconverted " + count + " files in " + (float)(t21 - t1) / 1000.0F + " s.");
        }
    }*/

    private void showImageWriters() {
        System.out.println("ImageWriters for format name:" + this.formatName);
        int i = 0;

        for(Iterator it = ImageIO.getImageWritersByFormatName(this.formatName); it.hasNext(); System.out.println("-----------------------------")) {
            ImageWriter writer = (ImageWriter)it.next();
            System.out.println("Writer[" + i++ + "]: " + writer.getClass().getName() + ":");
            System.out.println("   Write Param:");
            ImageWriteParam param = writer.getDefaultWriteParam();
            System.out.println("       canWriteCompressed:" + param.canWriteCompressed());
            System.out.println("      canWriteProgressive:" + param.canWriteProgressive());
            System.out.println("            canWriteTiles:" + param.canWriteTiles());
            System.out.println("           canOffsetTiles:" + param.canOffsetTiles());
            if(param.canWriteCompressed()) {
                String[] types = param.getCompressionTypes();
                System.out.println("   Compression Types:");
                if(types != null && types.length > 0) {
                    for(int j = 0; j < types.length; ++j) {
                        System.out.println("           Type[" + j + "]:" + types[j]);
                    }
                }
            }
        }

    }

    private void showFormatNames() {
        System.out.println("List of supported Format Names of registered ImageWriters:");
        Iterator writers = ServiceRegistry.lookupProviders(ImageWriterSpi.class);
        HashSet allNames = new HashSet();

        while(writers.hasNext()) {
            String[] names = ((ImageWriterSpi)writers.next()).getFormatNames();

            for(int i$ = 0; i$ < names.length; ++i$) {
                allNames.add(names[i$].toUpperCase());
            }
        }

        System.out.print("   Found " + allNames.size() + " format names: ");
        Iterator var6 = allNames.iterator();

        while(var6.hasNext()) {
            String n = (String)var6.next();
            System.out.print("\'" + n + "\', ");
        }

        System.out.println();
    }

    private static DicomObject loadDicomObject(File file) {
        DicomInputStream in = null;

        DicomObject e;
        try {
            in = new DicomInputStream(file);
            e = in.readDicomObject();
        } catch (IOException var6) {
            exit(var6.getMessage());
            throw new RuntimeException();
        } finally {
            CloseUtils.safeClose(in);
        }

        return e;
    }

    private static short[] loadPVal2Gray(File file) {
        BufferedReader r = null;

        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            short[] e = new short[256];
            int n = 0;

            String line;
            while((line = r.readLine()) != null) {
                try {
                    int nfe = Integer.parseInt(line.trim());
                    if(n == e.length) {
                        if(n == 65536) {
                            exit("Number of entries in " + file + " > 2^16");
                        }

                        short[] tmp = e;
                        e = new short[n << 1];
                        System.arraycopy(tmp, 0, e, 0, n);
                    }

                    e[n++] = (short)nfe;
                } catch (NumberFormatException var11) {
                    ;
                }
            }

            if(n != e.length) {
                exit("Number of entries in " + file + ": " + n + " != 2^[8..16]");
            }

            short[] var14 = e;
            return var14;
        } catch (IOException var12) {
            exit(var12.getMessage());
            throw new RuntimeException();
        } finally {
            CloseUtils.safeClose(r);
        }
    }

    private static CommandLine parse(String[] args) {
        Options opts = new Options();
        OptionBuilder.withArgName("frame");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("frame to convert, 1 (= first frame) by default");
        opts.addOption(OptionBuilder.create("f"));
        OptionBuilder.withArgName("imagequality");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("JPEG Image Quality (0-100)");
        opts.addOption(OptionBuilder.create("q"));
        OptionBuilder.withArgName("ImageWriterClass");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("ImageWriter to be used. Use the first ImageIO Writer found for given image format by default");
        opts.addOption(OptionBuilder.create("imagewriter"));
        opts.addOption("S", "showFormats", false, "Show all supported format names by registered ImageWriters.");
        opts.addOption("s", "showimagewriter", false, "Show all available Image Writer for specified format name.");
        OptionBuilder.withArgName("formatName");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Image Format Name. [default JPEG] This option will imply default values for jpgext=\'.<formatname>\'");
        opts.addOption(OptionBuilder.create("F"));
        OptionBuilder.withArgName("compressionType");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Compression Type. [default: \'*\' (exception: jpeg for format JPEG)] Use * to choose the first compression type.");
        opts.addOption(OptionBuilder.create("T"));
        OptionBuilder.withArgName("prfile");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("file path of presentation state to apply");
        opts.addOption(OptionBuilder.create("p"));
        OptionBuilder.withArgName("center");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Window Center");
        opts.addOption(OptionBuilder.create("c"));
        OptionBuilder.withArgName("width");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("Window Width");
        opts.addOption(OptionBuilder.create("w"));
        opts.addOption("sigmoid", false, "apply sigmoid VOI LUT function with given Window Center/Width");
        opts.addOption("noauto", false, "disable auto-windowing for images w/o VOI attributes");
        OptionBuilder.withArgName("file");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("file path of P-Value to gray value map");
        opts.addOption(OptionBuilder.create("pv2gray"));
        OptionBuilder.withArgName(".xxx");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("jpeg file extension used with destination directory argument, default: \'.jpg\'.");
        opts.addOption(OptionBuilder.create("jpgext"));
        opts.addOption("h", "help", false, "print this message");
        opts.addOption("V", "version", false, "print the version information and exit");
        CommandLine cl = null;

        try {
            cl = (new GnuParser()).parse(opts, args);
        } catch (ParseException var4) {
            exit("dcm2jpg: " + var4.getMessage());
            throw new RuntimeException("unreachable");
        }

        if(cl.hasOption('V')) {
            Package formatter = Dcm2JpgUtils.class.getPackage();
            System.out.println("dcm2jpg v" + formatter.getImplementationVersion());
            System.exit(0);
        }

        if(cl.hasOption('h') || !cl.hasOption('s') && !cl.hasOption('S') && cl.getArgList().size() < 2) {
            HelpFormatter formatter1 = new HelpFormatter();
            formatter1.printHelp("dcm2jpg [Options] <dcmfile> <jpegfile>\nor dcm2jpg [Options] <dcmfile>... <outdir>\nor dcm2jpg [Options] <indir>... <outdir>", "Convert DICOM image(s) to JPEG(s)\nOptions:", opts, EXAMPLE);
            System.exit(0);
        }

        return cl;
    }

    private static int parseInt(String s, String errPrompt, int min, int max) {
        try {
            int e = Integer.parseInt(s);
            if(e >= min && e <= max) {
                return e;
            }
        } catch (NumberFormatException var5) {
            ;
        }

        exit(errPrompt);
        throw new RuntimeException();
    }

    private static float parseFloat(String s, String errPrompt) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException var3) {
            exit(errPrompt);
            throw new RuntimeException();
        }
    }

    private static void exit(String msg) {
        System.err.println(msg);
        System.err.println("Try \'dcm2jpg -h\' for more information.");
        System.exit(1);
    }
}
