// IUpdateCallback.aidl
package com.flyme.update.helper.interfaces;

interface IUpdateCallback {
      void onPayloadApplicationComplete(int errorCode);

      void onStatusUpdate(int status, float percent);
}