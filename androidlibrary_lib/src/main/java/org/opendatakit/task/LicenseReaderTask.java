/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.task;

import android.app.Application;
import android.os.AsyncTask;

import org.opendatakit.androidlibrary.R;
import org.opendatakit.listener.LicenseReaderListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicenseReaderTask extends AsyncTask<Void, Integer, String> {

  private Application appContext;
  private LicenseReaderListener lrl;
  private String appName;
  private String mResult;

  protected String doInBackground(Void... args) {

    StringBuilder interimResult = null;

    try {
      InputStream licenseInputStream = appContext.getResources().openRawResource(R.raw.license);
      InputStreamReader licenseInputStreamReader = new InputStreamReader(licenseInputStream);
      BufferedReader r = new BufferedReader(licenseInputStreamReader);
      interimResult = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) {
        interimResult.append(line);
        interimResult.append("\n");
      }
      r.close();
      licenseInputStreamReader.close();
      licenseInputStream.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return (interimResult == null) ? null : interimResult.toString();
  }

  @Override
  protected void onPostExecute(String result) {
    synchronized (this) {
      mResult = result;
      appContext = null;
      if (lrl != null) {
        lrl.readLicenseComplete(result);
      }
    }
  }

  @Override
  protected void onCancelled(String result) {
    synchronized (this) {
      mResult = result;
      appContext = null;
      // can be null if cancelled before task executes
      if (lrl != null) {
        lrl.readLicenseComplete(result);
      }
    }
  }

  public String getResult() {
    return mResult;
  }

  public void setLicenseReaderListener(LicenseReaderListener listener) {
    synchronized (this) {
      lrl = listener;
    }
  }

  public void clearLicenseReaderListener(LicenseReaderListener listener) {
    synchronized (this) {
      if (lrl == listener) {
        lrl = null;
      }
    }
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    synchronized (this) {
      this.appName = appName;
    }
  }

  public Application getApplication() {
    return appContext;
  }

  public void setApplication(Application appContext) {
    synchronized (this) {
      this.appContext = appContext;
    }
  }
}

