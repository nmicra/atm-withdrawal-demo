package com.github.nmicra.atmwithdrawl.util;

import com.github.nmicra.atmwithdrawl.pojo.WithdrawalRestrictionState;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileUtils {


    public static void serializeConcurrentHashMap(ConcurrentHashMap<String, WithdrawalRestrictionState> map, String fileName) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(fileName);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(map);
        }
    }


    public static ConcurrentHashMap<String, WithdrawalRestrictionState> deserializeConcurrentHashMap(String fileName) throws IOException, ClassNotFoundException {
        try (FileInputStream fileInputStream = new FileInputStream(fileName);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (ConcurrentHashMap<String, WithdrawalRestrictionState>) objectInputStream.readObject();
        }
    }
}
