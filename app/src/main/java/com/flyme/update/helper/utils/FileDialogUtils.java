package com.flyme.update.helper.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flyme.update.helper.R;
import com.flyme.update.helper.adapter.FileAdapter;
import com.flyme.update.helper.bean.FileBean;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.dialogs.FullScreenDialog;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.interfaces.BaseDialog;
import com.kongzue.dialogx.interfaces.OnBackPressedListener;
import com.kongzue.dialogx.interfaces.OnBindView;
import com.kongzue.dialogx.interfaces.OnMenuItemClickListener;
import com.kongzue.filedialog.interfaces.FileSelectCallBack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileDialogUtils {

    public static FileDialogUtils build() {
        return new FileDialogUtils();
    }

    public static int REQUEST_PERMISSION_CODE = 9103;

    String[] mimeTypes;
    String[] suffixArray;
    FileSelectCallBack fileSelectCallBack;
    int maxSelectionNumber = 1;
    boolean showFileDate = true;

    enum SelectType {
        FILE,
        FOLDER
    }

    SelectType selectType = SelectType.FILE;
    FullScreenDialog dialog;

    public void selectFileByMime(String mimeType, FileSelectCallBack fileSelectCallBack) {
        selectFileByMime(new String[]{mimeType}, fileSelectCallBack);
    }

    public void selectFileBySuffix(String suffix, FileSelectCallBack fileSelectCallBack) {
        selectFileBySuffix(new String[]{suffix}, fileSelectCallBack);
    }

    public void selectFile(FileSelectCallBack fileSelectCallBack) {
        selectType = SelectType.FILE;
        this.fileSelectCallBack = fileSelectCallBack;
        readyShowDialog();
    }

    public void selectFileByMime(String[] mimeTypes, FileSelectCallBack fileSelectCallBack) {
        selectType = SelectType.FILE;
        this.mimeTypes = mimeTypes;
        this.fileSelectCallBack = fileSelectCallBack;
        readyShowDialog();
    }

    public void selectFileBySuffix(String[] suffixArray, FileSelectCallBack fileSelectCallBack) {
        selectType = SelectType.FILE;
        this.suffixArray = suffixArray;
        this.fileSelectCallBack = fileSelectCallBack;
        readyShowDialog();
    }

    public void selectFolder(FileSelectCallBack fileSelectCallBack) {
        selectType = SelectType.FOLDER;
        this.fileSelectCallBack = fileSelectCallBack;
        readyShowDialog();
    }

    private void readyShowDialog() {
        Context context = BaseDialog.getTopActivity();
        if (!(context instanceof Activity)) {
            errorLog("请先完成 DialogX 组件的初始化，详情：https://github.com/kongzue/DialogX");
            return;
        }
        requestPermissions(context);
    }

    /**
     * 建议自行处理好权限问题，不要依赖 FileDialog 申请，原因是无法监听 onRequestPermissionsResult 的回调.
     * 若实在懒得自己处理，请重写 activity 的 onRequestPermissionsResult(...) 方法，
     * 并调用 FileDialog 实例的 onRequestPermissionsResult(...) 方法。
     *
     * @param context activity 上下文
     */
    private void requestPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                createDialog();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                ((Activity) context).startActivityForResult(intent, REQUEST_PERMISSION_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                createDialog();
            } else {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
            }
        } else {
            createDialog();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            readyShowDialog();
        }
    }

    String path;
    FileAdapter fileAdapter;
    List<String> selectPathList;
    String title;
    String baseTitle;
    int sortType;

    private void createDialog() {
        dialog = FullScreenDialog.build();
        dialog.setCustomView(new OnBindView<FullScreenDialog>(R.layout.dialog_file_select) {

            private TextView btnCancel;
            private TextView txtDialogTitle;
            private ImageView btnSort;
            private TextView txtPath;
            private ListView listFile;
            private ImageView imgFileListScreenshot;

            @Override
            public void onBind(FullScreenDialog dialog, View v) {
                FileDialogUtils.this.dialog = dialog;

                btnCancel = v.findViewById(R.id.btn_cancel);
                txtDialogTitle = v.findViewById(R.id.txt_dialog_title);
                btnSort = v.findViewById(R.id.btn_sort);
                txtPath = v.findViewById(R.id.txt_path);
                listFile = v.findViewById(R.id.list_file);
                imgFileListScreenshot = v.findViewById(R.id.img_file_list_screenshot);

                if (TextUtils.isEmpty(title)) {
                    if (selectType == SelectType.FILE) {
                        if (getMaxSelectionNumber() == 1) {
                            txtDialogTitle.setText(BaseDialog.getTopActivity().getResources().getText(com.kongzue.filedialog.R.string.dialogx_filedialog_title_file));
                        } else {
                            baseTitle = BaseDialog.getTopActivity().getResources().getString(com.kongzue.filedialog.R.string.dialogx_filedialog_title_file);
                            txtDialogTitle.setText(baseTitle + "(0/" + getMaxSelectionNumber() + ")");
                        }
                    } else {
                        txtDialogTitle.setText(BaseDialog.getTopActivity().getResources().getText(com.kongzue.filedialog.R.string.dialogx_filedialog_title_folder));
                    }
                } else {
                    txtDialogTitle.setText(title);
                }

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                btnSort.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BottomMenu.show("按名称", "按大小", "按时间", "按类型")
                                .setMessage("显示文件排序的方式")
                                .setTitle("排序方式")
                                .setOnMenuItemClickListener(new OnMenuItemClickListener<BottomMenu>() {
                                    @Override
                                    public boolean onClick(BottomMenu dialog, CharSequence text, int index) {
                                        //记录已选择值
                                        sortType = index;
                                        return false;
                                    }
                                })
                                .setSelection(sortType);
                    }
                });

//                if (selectType == SelectType.FILE && getMaxSelectionNumber() == 1) {
//                    btnSelect.setFocusable(false);
//                    btnSelect.setClickable(false);
//                    btnSelect.setVisibility(View.INVISIBLE);
//                }

                dialog.setOnBackPressedListener(new OnBackPressedListener<FullScreenDialog>() {
                    @Override
                    public boolean onBackPressed(FullScreenDialog dialog) {
                        if (!Environment.getExternalStorageDirectory().getPath().equals(path)) {
                            if (path.contains("/")) {
                                String[] folders = path.split("/");
                                if (folders.length > 2) {
                                    path = "";
                                    for (int i = 0; i < folders.length - 1; i++) {
                                        path = path + (i == 0 ? "" : "/") + folders[i];
                                    }

                                    Bitmap screenshot = screenshotView(listFile);
                                    imgFileListScreenshot.setImageBitmap(screenshot);

                                    imgFileListScreenshot.setVisibility(View.VISIBLE);
                                    imgFileListScreenshot.setX(0);

                                    refreshFileList();

                                    listFile.setX(-listFile.getWidth());
                                    imgFileListScreenshot.animate().setInterpolator(new DecelerateInterpolator(2f)).x(listFile.getWidth());
                                    listFile.animate().setInterpolator(new DecelerateInterpolator(2f)).x(0);
                                }
                            }
                            return false;
                        }
                        return true;
                    }
                });

                loadFileList();
            }

            private void loadFileList() {
                if (TextUtils.isEmpty(path)) {
                    path = Environment.getExternalStorageDirectory().getPath();
                }
                fileAdapter = new FileAdapter(FileDialogUtils.this, BaseDialog.getTopActivity(), new ArrayList<>());
                listFile.setAdapter(fileAdapter);
                listFile.setOnItemClickListener((parent, view, position, id) -> {
                    view.setPressed(false);
                    String fileName = ((FileBean) fileAdapter.getItem(position)).Name;
                    if ("...".equals(fileName)) {
                        //返回上级
                        if (path.contains("/")) {
                            String[] folders = path.split("/");
                            if (folders.length > 2) {
                                path = "";
                                for (int i = 0; i < folders.length - 1; i++) {
                                    path = path + (i == 0 ? "" : "/") + folders[i];
                                }

                                Bitmap screenshot = screenshotView(listFile);
                                imgFileListScreenshot.setImageBitmap(screenshot);

                                imgFileListScreenshot.setVisibility(View.VISIBLE);
                                imgFileListScreenshot.setX(0);

                                refreshFileList();

                                listFile.setX(-listFile.getWidth());
                                imgFileListScreenshot.animate().setInterpolator(new DecelerateInterpolator(2f)).x(listFile.getWidth());
                                listFile.animate().setInterpolator(new DecelerateInterpolator(2f)).x(0);
                            }
                        }
                    } else {
                        boolean isFolder = fileName.startsWith("/");
                        String itemPath = path + (isFolder ? "" : "/") + fileName;
                        if (isFolder) {
                            //点击文件夹
                            Bitmap screenshot = screenshotView(listFile);
                            imgFileListScreenshot.setImageBitmap(screenshot);

                            imgFileListScreenshot.setVisibility(View.VISIBLE);
                            imgFileListScreenshot.setX(0);

                            path = itemPath;
                            refreshFileList();

                            listFile.setX(listFile.getWidth());
                            imgFileListScreenshot.animate().setInterpolator(new DecelerateInterpolator(2f)).x(-listFile.getWidth());
                            listFile.animate().setInterpolator(new DecelerateInterpolator(2f)).x(0);
                        } else {
                            //点击文件
                            if (getMaxSelectionNumber() != 1) {
                                //多选
                                if (selectPathList == null) {
                                    selectPathList = new ArrayList<String>();
                                }
                                if (selectPathList.contains(itemPath)) {
                                    selectPathList.remove(itemPath);
                                } else {
                                    if (selectPathList.size() >= getMaxSelectionNumber()) {
                                        PopTip.show(String.format(
                                                BaseDialog.getTopActivity().getResources().getString(com.kongzue.filedialog.R.string.error_dialogx_filedialog_max_selection_tip),
                                                String.valueOf(getMaxSelectionNumber())
                                        ));
                                        return;
                                    }
                                    selectPathList.add(itemPath);
                                }
                                fileAdapter.notifyDataSetChanged();
                                baseTitle = BaseDialog.getTopActivity().getResources().getString(com.kongzue.filedialog.R.string.dialogx_filedialog_title_file);
                                txtDialogTitle.setText(baseTitle + "(" + selectPathList.size() + "/" + getMaxSelectionNumber() + ")");
                            } else {
                                //选择文件
                                if (view.getAlpha() == 1f) {
                                    dialog.dismiss();
                                    fileSelectCallBack.onSelect(new File(itemPath), itemPath);
                                } else {
                                    PopTip.show(com.kongzue.filedialog.R.string.error_dialogx_filedialog_no_support_file_type_tip);
                                }
                            }
                        }
                    }
                });
                refreshFileList();
            }

            private void refreshFileList() {
                fileAdapter.setFileList(new ArrayList<>());
                fileAdapter.notifyDataSetChanged();
                new Thread(() -> {
                    try {
                        ArrayList<FileBean> allFileArray = new ArrayList<FileBean>();
                        ArrayList<FileBean> folderArray = new ArrayList<FileBean>();
                        ArrayList<FileBean> fileArray = new ArrayList<FileBean>();



                        File file = new File(path);
                        File[] listFiles = file.listFiles();
                        if (listFiles != null && listFiles.length != 0) {
                            for (File f : listFiles) {
                                FileBean fileBean = new FileBean();
                                fileBean.lastModified = f.lastModified();
                                if (f.isDirectory()) {
                                    fileBean.Name = "/" + f.getName();
                                    folderArray.add(fileBean);
                                } else {
                                    fileBean.Name = f.getName();
                                    fileArray.add(fileBean);
                                }

                            }
                        }
                        if (!Environment.getExternalStorageDirectory().getPath().equals(path)) {
                            FileBean fileBean = new FileBean();
                            fileBean.Name = "...";
                            fileBean.lastModified = 0;
                            allFileArray.add(fileBean);
                        }
                        allFileArray.addAll(folderArray);
                        if (selectType == SelectType.FILE) {
                            allFileArray.addAll(fileArray);
                        }

                        BaseDialog.getTopActivity().runOnUiThread(() -> {
                            txtPath.setText(path);
                            fileAdapter.setFileList(allFileArray);
                            fileAdapter.notifyDataSetChanged();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
        dialog.show();
    }

    public FullScreenDialog getDialog() {
        return dialog;
    }

    public FileDialogUtils setDialog(FullScreenDialog dialog) {
        this.dialog = dialog;
        return this;
    }

    private void errorLog(String msg) {
        Log.e(">>>", "FileDialog: " + msg);
    }

    public int getMaxSelectionNumber() {
        return maxSelectionNumber;
    }

    public FileDialogUtils setMaxSelectionNumber(int maxSelectionNumber) {
        this.maxSelectionNumber = maxSelectionNumber;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FileDialogUtils setTitle(String title) {
        this.title = title;
        return this;
    }

    private Bitmap screenshotView(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    public FileDialogUtils setMimeTypes(String[] mimeTypes) {
        this.mimeTypes = mimeTypes;
        return this;
    }

    public FileDialogUtils setSuffixArray(String[] suffixArray) {
        this.suffixArray = suffixArray;
        return this;
    }

    public String[] getMimeTypes() {
        return mimeTypes;
    }

    public String[] getSuffixArray() {
        return suffixArray;
    }

    public String getPath() {
        return path;
    }

    public FileDialogUtils setPath(String path) {
        this.path = path;
        return this;
    }

    public FileDialogUtils setPath(File folder) {
        if (folder.isDirectory()) {
            this.path = folder.getAbsolutePath();
        }else{
            this.path = folder.getParent();
        }
        return this;
    }

    public List<String> getSelectPathList() {
        return selectPathList;
    }

    public boolean isShowFileDate() {
        return showFileDate;
    }

    public FileDialogUtils setShowFileDate(boolean showFileDate) {
        this.showFileDate = showFileDate;
        return this;
    }
}
