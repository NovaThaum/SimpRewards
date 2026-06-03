package Simpmc.Rewards.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JsonStorage {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void save(File folder, PlayerData data) {

        try {

            File file = new File(folder, data.getUuid() + ".json");

            Writer writer = new OutputStreamWriter(
                    new FileOutputStream(file),
                    StandardCharsets.UTF_8
            );

            GSON.toJson(data, writer);

            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PlayerData load(File folder, UUID uuid) {

        try {

            File file = new File(folder, uuid + ".json");

            if (!file.exists()) {
                return null;
            }

            Reader reader = new InputStreamReader(
                    new FileInputStream(file),
                    StandardCharsets.UTF_8
            );

            PlayerData data = GSON.fromJson(reader, PlayerData.class);

            reader.close();

            return data;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
