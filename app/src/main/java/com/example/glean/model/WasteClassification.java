package com.example.glean.model;

public class WasteClassification {
    public enum WasteType {
        ORGANIC("Organik"),
        INORGANIC("Anorganik"),
        HAZARDOUS("B3 (Berbahaya & Beracun)");

        private final String displayName;

        WasteType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private WasteType type;
    private float confidence;
    private String description;
    private String tips;

    public WasteClassification(WasteType type, float confidence, String description, String tips) {
        this.type = type;
        this.confidence = confidence;
        this.description = description;
        this.tips = tips;
    }

    public WasteType getType() { return type; }
    public void setType(WasteType type) { this.type = type; }

    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }
    
    // Helper method to get appropriate tips based on waste type
    public static String getDefaultTipsForType(WasteType type) {
        switch (type) {
            case ORGANIC:
                return "• Jadikan kompos untuk tanaman\n" +
                       "• Gunakan untuk pupuk alami\n" +
                       "• Pisahkan dari sampah anorganik\n" +
                       "• Simpan di tempat tertutup untuk mengurangi bau";
                
            case HAZARDOUS:
                return "• Jangan dibuang bersama sampah biasa\n" +
                       "• Kumpulkan di tempat khusus sampah B3\n" +
                       "• Serahkan ke pusat daur ulang atau tempat pengolahan B3\n" +
                       "• Hindari membakar atau menguburnya";
                
            case INORGANIC:
                return "• Pisahkan berdasarkan jenisnya (plastik, kertas, logam, kaca)\n" +
                       "• Bersihkan dulu sebelum didaur ulang\n" +
                       "• Manfaatkan untuk kerajinan tangan\n" +
                       "• Serahkan ke bank sampah terdekat";
                
            default:
                return "Kelola sampah ini sesuai dengan jenisnya dan peraturan setempat.";
        }
    }
    
    // Helper method to get appropriate descriptions based on waste type
    public static String getDefaultDescriptionForType(WasteType type) {
        switch (type) {
            case ORGANIC:
                return "Sampah organik adalah sampah yang berasal dari makhluk hidup dan dapat terurai " +
                       "secara alami. Contohnya adalah sisa makanan, daun, ranting, dan sebagainya.";
                
            case HAZARDOUS:
                return "Sampah B3 (Bahan Berbahaya dan Beracun) adalah sampah yang mengandung zat berbahaya " +
                       "atau beracun yang dapat membahayakan lingkungan dan kesehatan manusia. Contohnya " +
                       "adalah baterai, lampu neon, obat kadaluarsa, dan sebagainya.";
                
            case INORGANIC:
                return "Sampah anorganik adalah sampah yang sulit atau tidak bisa terurai secara alami. " +
                       "Contohnya adalah plastik, kaca, logam, kertas, dan sebagainya. Sebagian besar " +
                       "sampah anorganik dapat didaur ulang.";
                
            default:
                return "Sampah ini perlu dikelola dengan tepat sesuai dengan jenisnya.";
        }
    }
}