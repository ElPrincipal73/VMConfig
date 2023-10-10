package de.comline;

public class VNetworkAdapterModel {
    private String vNetworkAdapter;
    private String vNetworkVISDKServer;
    private String vNetworkHost;
    private String importDate;
    private Integer uploadIndex;

    // Getter und Setter Methoden für alle Eigenschaften
    public String getVNetworkAdapter() {
        return vNetworkAdapter;
    }

    public void setVNetworkAdapter(String vNetworkAdapter) {
        this.vNetworkAdapter = vNetworkAdapter;
    }

    public String getVNetworkVISDKServer() {
        return vNetworkVISDKServer;
    }

    public void setVNetworkVISDKServer(String vNetworkVISDKServer) {
        this.vNetworkVISDKServer = vNetworkVISDKServer;
    }

    public String getVNetworkHost() {
        return vNetworkHost;
    }

    public void setVNetworkHost(String vNetworkHost) {
        this.vNetworkHost = vNetworkHost;
    }

    public String getImportDate() {
        return importDate;
    }

    public void setImportDate(String importDate) {
        this.importDate = importDate;
    }

    public Integer getUploadIndex() {
        return uploadIndex;
    }

    public void setUploadIndex(Integer uploadIndex) {
        this.uploadIndex = uploadIndex;
    }
}
