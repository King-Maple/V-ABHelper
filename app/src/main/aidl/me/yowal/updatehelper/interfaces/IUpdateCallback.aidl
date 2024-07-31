// IUpdateCallback.aidl
package me.yowal.updatehelper.interfaces;

import me.yowal.updatehelper.interfaces.IUpdateCallback;

interface IUpdateCallback {
      void onPayloadApplicationComplete(int errorCode);

      void onStatusUpdate(int status, float percent);
}