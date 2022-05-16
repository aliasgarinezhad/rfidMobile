package com.jeanwest.mobile.testClasses;

import android.content.Context;

import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.exception.ConfigurationException;

import java.util.ArrayList;

public class RFIDWithUHFUART {

    public static ArrayList<UHFTAGInfo> uhfTagInfo = new ArrayList<>(2100);
    public static UHFTAGInfo writtenUhfTagInfo = new UHFTAGInfo();

    int i = 0;

    public static synchronized RFIDWithUHFUART getInstance() throws ConfigurationException {
        return new RFIDWithUHFUART();
    }

    public synchronized boolean init() {
        return true;
    }

    public synchronized boolean init(Context context) {
        return true;
    }

    public synchronized boolean free() {
        return true;
    }

    public synchronized int getPower() {
        return 10;
    }

    public synchronized boolean setPower(int power) {
        return true;
    }

    public synchronized int getFrequencyMode() {
        return 50;
    }

    public synchronized boolean setFrequencyMode(int freMode) {
        return true;
    }

    public synchronized boolean startInventoryTag() {
        return true;
    }

    public synchronized boolean startInventoryTag(int flagAnti, int initQ, int tidLen) {
        return true;
    }

    public UHFTAGInfo readTagFromBuffer() {

        UHFTAGInfo result;

        if (i < uhfTagInfo.size()) {

            result = uhfTagInfo.get(i);
            i++;

        } else {
            result = null;
            i = 0;
            uhfTagInfo.clear();
        }

        return result;
    }

    public synchronized boolean stopInventory() {
        return true;
    }

    public synchronized UHFTAGInfo inventorySingleTag() {

        UHFTAGInfo result;

        if (i < uhfTagInfo.size()) {

            result = uhfTagInfo.get(i);
            i++;

        } else {
            result = null;
            i = 0;
            uhfTagInfo.clear();
        }

        return result;
    }

    public String readData(String accessPwd, int bank, int ptr, int cnt) {
        return "";
    }

    public String readData(String accessPwd, int filterBank, int filterPtr, int filterCnt, String filterData, int bank, int ptr, int cnt) {

        if (filterData.equals(writtenUhfTagInfo.getTid())) {
            return writtenUhfTagInfo.getEPC();
        } else if (uhfTagInfo.get(0).getTid().equals(filterData)) {
            return uhfTagInfo.get(0).getEPC();
        } else {
            return "";
        }
    }

    public boolean writeData(String accessPwd, int bank, int ptr, int cnt, String data) {
        return true;
    }

    public boolean writeData(String accessPwd, int filterBank, int filterPtr, int filterCnt, String filterData, int bank, int ptr, int cnt, String writeData) {

        writtenUhfTagInfo.setEPC(writeData);
        writtenUhfTagInfo.setTid(filterData);
        return true;
    }

    public synchronized boolean setProtocol(int protocol) {
        return true;
    }

    public synchronized boolean setEPCMode() {
        return true;
    }

    public synchronized boolean setEPCAndTIDMode() {
        return true;
    }

    public synchronized boolean setRFLink(int link) {
        return true;
    }

    public synchronized boolean setEPCAndTIDUserMode(int user_prt, int user_len) {
        return true;
    }
}

