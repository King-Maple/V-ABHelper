package com.flyme.update.helper.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidInfo {

    private final Context context;

    public AndroidInfo(Context context){
        this.context = context;
    }

    /**
     * 获取当前本地apk的版本
     *
     * @return 版本号
     */
    public int getVersionCode() {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 获取版本号名称
     *
     * @return 版本名称
     */
    public String getVerName() {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    public static boolean isRoot(){
        try {
            Process process  = Runtime.getRuntime().exec("su");
            OutputStream outputStream = process.getOutputStream();
            outputStream.write("exit\n".getBytes());
            outputStream.flush();
            int i = process.waitFor();
            outputStream.close();
            return i == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public double getDiveSize(){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        // 屏幕尺寸
        BigDecimal decimal = BigDecimal.valueOf(Math.sqrt(x + y));
        decimal = decimal.setScale(2,BigDecimal.ROUND_UP);
        return decimal.doubleValue();
    }

    public String getCpuName() {
        String cpu = "";
        try {
            String str1 = "/proc/cpuinfo";
            String[] cpuInfo = { "", "" };
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            String line = null;
            while ((line = localBufferedReader.readLine()) != null) {
                if (line.toLowerCase().contains("hardware")) {
                    cpuInfo[0] = line;
                    break;
                }
            }
            cpuInfo[1] = Build.HARDWARE;
            localBufferedReader.close();
            cpu = cpuInfo[0] + "&" + cpuInfo[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (cpu.contains("Inc")){
                cpu = cpu.substring(cpu.indexOf("Inc ")+4).replace("&", " By ");
                cpu = cpu.substring(0, cpu.indexOf(" "));
            }else {
                cpu = cpu.substring(cpu.indexOf("dware :")+7);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return cpu;
    }

    public String getBrand(){
        return Build.BRAND;
    }

    public String getModel(){
        return Build.MODEL;
    }

    public int getSDK(){
        return Build.VERSION.SDK_INT;
    }

    public String getRelease(){
        return Build.VERSION.RELEASE;
    }

    public String getDevice(){
        return Build.DEVICE;
    }

    public String getHardWare(){
        return Build.HARDWARE;
    }

    public String getKernel() {
        String result = "";
        String line;
        String[] cmd = new String[] { "/system/bin/cat", "/proc/version" };
        String workdirectory = "/system/bin/";
        try {
            ProcessBuilder bulider = new ProcessBuilder(cmd);
            bulider.directory(new File(workdirectory));
            bulider.redirectErrorStream(true);
            Process process = bulider.start();
            InputStream in = process.getInputStream();
            InputStreamReader isrout = new InputStreamReader(in);
            BufferedReader brout = new BufferedReader(isrout, 8 * 1024);

            while ((line = brout.readLine()) != null) {
                result += line;
                // result += "\n";
            }
            in.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("TAG","----Linux Kernal is :"+result);
        return result;
    }

    public int width(){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        // 可能有虚拟按键的情况
        display.getRealSize(outPoint);
        return outPoint.x;
    }

    public int height(){
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        // 可能有虚拟按键的情况
        display.getRealSize(outPoint);

        return outPoint.y;
    }

    /** 获取当前可用运行内存大小 */
    public String getAvailMemory() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
    }


    /** 获取android总运行内存大小 */
    public String getTotalMemory() {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            arrayOfString = str2.split("\\s+");
            int i = Integer.valueOf(arrayOfString[1]);// 获得系统总内存，单位是KB
            initial_memory = (long) i * 1024;//int值乘以1024转换为long类型
            localBufferedReader.close();
        } catch (IOException ignored) {
        }
        return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化
    }

    /** 获取手机内部存储空间，以M,G为单位的容量 */
    public String getInternalMemorySize() {
        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long blockCountLong = statFs.getBlockCountLong();
        long size = blockCountLong * blockSizeLong;
        return Formatter.formatFileSize(context, size);
    }

    /** 获取手机内部可用存储空间，以M,G为单位的容量 */
    public String getAvailableInternalMemorySize() {
        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        long availableBlocksLong = statFs.getAvailableBlocksLong();
        long blockSizeLong = statFs.getBlockSizeLong();
        return Formatter.formatFileSize(context, availableBlocksLong * blockSizeLong);
    }

    public String getBatteryCapacity() {
        double var1 = (double)0;
        double var5;
        try {
            @SuppressLint("PrivateApi") Class var3 = Class.forName("com.android.internal.os.PowerProfile");
            Class var4;
            try {
                var4 = Class.forName("android.content.Context");
            } catch (ClassNotFoundException var7) {
                NoClassDefFoundError var9 = new NoClassDefFoundError(var7.getMessage());
                throw var9;
            }
            Object var10 = var3.getConstructor(var4).newInstance(context);
            var5 = (Double)Class.forName("com.android.internal.os.PowerProfile").getMethod("getBatteryCapacity").invoke(var10);
        } catch (Exception var8) {
            var8.printStackTrace();
            return var1 + " mAh";
        }
        var1 = var5;
        return var1 + "mAh";
    }

    @SuppressLint("DefaultLocale")
    public String getRefreshRote(Activity activity){
        Display display = activity.getWindowManager().getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        return String.format("%.2f", refreshRate) + "Hz";
    }

    private String cpuDir = "/sys/devices/system/cpu/";

    public int cpuCoreNum(){
        File file = new File(cpuDir);
        int num = 0;
        for (String i : Objects.requireNonNull(file.list())){
            if(i.contains("cpu0") || i.contains("cpu2") || i.contains("cpu3") || i.contains("cpu4")
                    || i.contains("cpu5") || i.contains("cpu6") || i.contains("cpu7") || i.contains("cpu8") || i.contains("cpu9") || i.contains("cpu1")){
                num = num + 1;
            }
        }
        return num;
    }

    //获取最大频率
    public String getMaxCpuFreq(int number) {
        String result = "";
        ProcessBuilder cmd;
        try {
            String[] args = { "/system/bin/cat",
                    "/sys/devices/system/cpu/cpu"+number+"/cpufreq/cpuinfo_max_freq" };
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        }
        return result.trim();
    }

    // 获取CPU最小频率
    public String getMinCpuFreq(int number) {
        String result = "";
        ProcessBuilder cmd;
        try {
            String[] args = { "/system/bin/cat",
                    "/sys/devices/system/cpu/cpu"+number+"/cpufreq/cpuinfo_min_freq" };
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        }
        return result.trim();
    }

    // 实时获取CPU当前频率
    public String getCurCpuFreq(int number) {
        String result = "N/A";
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        nf.setMaximumFractionDigits(0);
        try {
            FileReader fr = new FileReader(
                    "/sys/devices/system/cpu/cpu"+number+"/cpufreq/scaling_cur_freq");
            BufferedReader br = new BufferedReader(fr);
            String text = br.readLine();
            result = text;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /*
     *cpu频率转换单位
     */
    public String zh(String agr){
        String a = "0";
        try{
            float i = 1000;
            try{
                i = Float.parseFloat(agr);
            }catch (Exception e){
                e.printStackTrace();
            }
            a = String.valueOf(i/1000);
        }catch (Exception e){
            a = "0";
        }
        return a;
    }

    public String GetCPU() {
        String cpu = "";
        try {
            String str1 = "/proc/cpuinfo";
            String[] cpuInfo = { "", "" };
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            String line = null;
            while ((line = localBufferedReader.readLine()) != null) {
                if (line.toLowerCase().contains("hardware")) {
                    cpuInfo[0] = line;
                    break;
                }
            }
            cpuInfo[1] = Build.HARDWARE;
            localBufferedReader.close();
            cpu = cpuInfo[0] + "&" + cpuInfo[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cpu;
    }

    //String str = com.omarea.a.g.e.a.a(path);
    public static double getCPURateDesc(int id){
        String path = "/proc/stat";// 系统CPU信息文件
        long[] totalJiffies =new long[2];
        long[] totalIdle =new long[2];
        int firstCPUNum=0;//设置这个参数，这要是防止两次读取文件获知的CPU数量不同，导致不能计算。这里统一以第一次的CPU数量为基准
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        Pattern pattern=Pattern.compile(" [0-9]+");
        for(int i=0;i<2;i++) {
            try {
                fileReader = new FileReader(path);
                bufferedReader = new BufferedReader(fileReader, 8192);
                int currentCPUNum=0;
                String str;
                while ((str = bufferedReader.readLine()) != null&&(i==0||currentCPUNum<firstCPUNum)) {
                    if (str.toLowerCase().startsWith("cpu" + id)) {
                        currentCPUNum++;
                        int index = 0;
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            try {
                                long tempJiffies = Long.parseLong(Objects.requireNonNull(matcher.group(0)).trim());
                                totalJiffies[i] += tempJiffies;
                                if (index == 3) {//空闲时间为该行第4条栏目
                                    totalIdle[i] += tempJiffies;
                                }
                                index++;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(i==0){
                        firstCPUNum=currentCPUNum;
                        try {//暂停50毫秒，等待系统更新信息。
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        double rate=0;
        if (totalJiffies[0]>0&&totalJiffies[1]>0&&totalJiffies[0]!=totalJiffies[1]){
            rate=1.0*((totalJiffies[1]-totalIdle[1])-(totalJiffies[0]-totalIdle[0]))/(totalJiffies[1]-totalJiffies[0]);
        }

        return rate;
    }

    public static double getCPURateDesc(){
        String path = "/proc/stat";// 系统CPU信息文件
        long totalJiffies[]=new long[2];
        long totalIdle[]=new long[2];
        int firstCPUNum=0;//设置这个参数，这要是防止两次读取文件获知的CPU数量不同，导致不能计算。这里统一以第一次的CPU数量为基准
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        Pattern pattern=Pattern.compile(" [0-9]+");
        for(int i=0;i<2;i++) {
            totalJiffies[i]=0;
            totalIdle[i]=0;
            try {
                fileReader = new FileReader(path);
                bufferedReader = new BufferedReader(fileReader, 8192);
                int currentCPUNum=0;
                String str;
                while ((str = bufferedReader.readLine()) != null&&(i==0||currentCPUNum<firstCPUNum)) {
                    if (str.toLowerCase().startsWith("cpu")) {
                        currentCPUNum++;
                        int index = 0;
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            try {
                                long tempJiffies = Long.parseLong(matcher.group(0).trim());
                                totalJiffies[i] += tempJiffies;
                                if (index == 3) {//空闲时间为该行第4条栏目
                                    totalIdle[i] += tempJiffies;
                                }
                                index++;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(i==0){
                        firstCPUNum=currentCPUNum;
                        try {//暂停50毫秒，等待系统更新信息。
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        double rate=0;
        if (totalJiffies[0]>0&&totalJiffies[1]>0&&totalJiffies[0]!=totalJiffies[1]){
            rate=1.0*((totalJiffies[1]-totalIdle[1])-(totalJiffies[0]-totalIdle[0]))/(totalJiffies[1]-totalJiffies[0]);
        }

        return rate;
    }

    public static String getCpuTemp() {
        String temp = "--";
        BufferedReader br = null;
        FileReader fr = null;
        try {
            File dir = new File("/sys/class/thermal/");
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (Pattern.matches("thermal_zone[0-9]+", file.getName())) {
                        return true;
                    }
                    return false;
                }
            });

            final int SIZE = files.length;
            String line = "";
            String type = "";
            for (int i = 0; i < SIZE; i++) {
                fr = new FileReader("/sys/class/thermal/thermal_zone" + i + "/type");
                br = new BufferedReader(fr);
                line = br.readLine();
                if (line != null) {
                    type = line;
                }

                fr = new FileReader("/sys/class/thermal/thermal_zone" + i + "/temp");
                br = new BufferedReader(fr);
                line = br.readLine();
                if (line != null) {
                    // MTK CPU
                    if (type.contains("cpu")) {
                        long temperature = Long.parseLong(line);
                        if (temperature < 0) {
                            temp = "Unknow";
                        } else {
                            temp = (float) (temperature / 1000.0) + "";
                        }
                    } else if (type.contains("tsens_tz_sensor")) {
                        // Qualcomm CPU
                        long temperature = Long.parseLong(line);
                        if (temperature < 0) {
                            temp = "Unknow";
                        } else if (temperature > 100){
                            temp = (float) (temperature / 10.0) + "";
                        } else {
                            temp = temperature + "";
                        }
                    }

                }
            }

            if (fr != null) {
                fr.close();
            }
            if (br != null) {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return temp;
    }

}

