package com.unindra.restoclient.models;

import com.google.gson.annotations.Expose;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.unindra.restoclient.Client.*;
import static com.unindra.restoclient.Rupiah.rupiah;
import static com.unindra.restoclient.models.Level.level;
import static com.unindra.restoclient.models.Menu.menu;
import static com.unindra.restoclient.models.Setting.setting;

public class Pesanan extends RecursiveTreeObject<Pesanan> {
    private String id_pesanan;
    private String nama_menu;
    private int jumlah;
    private int level;
    private String no_meja;
    private String status_item;
    @Expose
    private static final String paramUrl = "/pesanan";
    @Expose
    private static ObservableList<Pesanan> pesananList = FXCollections.observableArrayList();

    // Constructor
    private Pesanan(String nama_menu, int jumlah, int lvl_item, String no_meja, String status_item) {
        this.id_pesanan = "";
        this.nama_menu = nama_menu;
        this.jumlah = jumlah;
        this.level = lvl_item;
        this.no_meja = no_meja;
        this.status_item = status_item;
    }

    public Pesanan(Menu menu, int jumlah) {
        this(menu.getNama_menu(), jumlah, 0, setting().getNo_meja(), "belum dipesan");
    }

    public Pesanan(Menu menu, int jumlah, int lvl_item) {
        this(menu.getNama_menu(), jumlah, lvl_item, setting().getNo_meja(), "belum dipesan");
    }

    // Sinkronisasi collections dengan server
    public static void updatePesanan() throws IOException {
        StandardResponse standardResponse = get(paramUrl + "/" + setting().getNo_meja());
        if (standardResponse.getStatus() == StatusResponse.SUCCESS) {
            Pesanan[] pesananArrays = gson().fromJson(standardResponse.getData(), Pesanan[].class);
            for (Pesanan pesanan : pesananArrays) pesanan.setChildren(FXCollections.observableArrayList());
            Pesanan.pesananList.setAll(pesananArrays);
        }
    }

    // Pesan
    public static boolean pesan() {
        getPesananList("belum dipesan").forEach(item -> item.status_item = "dipesan");

        return getPesananList("dipesan")
                .stream()
                .map(item -> item.put().getStatus() == StatusResponse.SUCCESS)
                .reduce(true, (a, b) -> a && b);
    }

    // Bayar
    public static boolean bayar() throws IOException {
        getPesananList("diproses").forEach(item -> item.status_item = "dibayar");

        boolean success = getPesananList("dibayar").stream()
                .map(item -> item.put().getStatus() == StatusResponse.SUCCESS)
                .reduce(true, (a, b) -> a && b);

        StandardResponse standardResponse = get("/bayar/" + setting().getNo_meja());
        return success && standardResponse.getStatus() == StatusResponse.SUCCESS;
    }

    // Post
    public StandardResponse post() {
        return send(paramUrl, "POST", gson().toJson(this));
    }

    // Put
    private StandardResponse put() {
        return send(paramUrl, "PUT", gson().toJson(this));
    }

    // Delete
    public StandardResponse delete() {
        return send(paramUrl, "DELETE", gson().toJson(this));
    }

    // Getter
    public static ObservableList<Pesanan> getPesananList() {
        return pesananList;
    }

    public static List<Pesanan> getPesananList(String status_item) {
        return pesananList
                .stream()
                .filter(pesanan -> pesanan.getStatus_item().equals(status_item))
                .collect(Collectors.toList());
    }

    private int getTotal() {
        try {
            return (menu(this)).getHarga_menu() + level(level).getHarga_level() * jumlah;
        } catch (IOException e) {
            return 0;
        }
    }

    public static int getGrandTotal() {
        return getPesananList().stream().mapToInt(Pesanan::getTotal).sum();
    }

    String getNama_menu() {
        return nama_menu;
    }

    public int getLevel() {
        return level;
    }

    public String getStatus_item() {
        return status_item;
    }

    // Property
    public ObjectProperty<Integer> jumlahProperty() {
        return new SimpleObjectProperty<>(jumlah);
    }

    public StringProperty totalProperty() {
        return new SimpleStringProperty(rupiah(getTotal()));
    }

    @Override
    public String toString() {
        return "Pesanan{" +
                "id_pesanan=" + id_pesanan +
                ", nama_menu=" + nama_menu +
                ", jumlah=" + jumlah +
                ", level=" + level +
                ", no_meja=" + no_meja +
                ", status_item='" + status_item + '\'' +
                '}';
    }
}
