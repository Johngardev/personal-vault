package io.sovereign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultData {
  public String did;
  public String ownerName;

  public String encodedPublicKey;
  public String encodedPrivateKey;

  public Map<String, Object> professionalProfile = new HashMap<>(); // GitHub/LinkedIn
  public List<String> cryptoWallets = new ArrayList<>(); // Direcciones de billeteras
  public Map<String, String> attributes = new HashMap<>(); //Metadatos generales

  public VaultData() {}
}
