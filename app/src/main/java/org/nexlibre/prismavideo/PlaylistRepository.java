package org.nexlibre.prismavideo;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Preserves the 2.0.1 JSON schema and preference key. Main-thread mutations. */
public final class PlaylistRepository {
 private final SharedPreferences preferences;
 private boolean damaged;
 public PlaylistRepository(SharedPreferences preferences){this.preferences=preferences;}
 private JSONObject read(){try{JSONObject value=new JSONObject(preferences.getString("playlists","{}"));damaged=false;return value;}catch(JSONException e){damaged=true;return new JSONObject();}}
 public List<String> names(){List<String> result=new ArrayList<>();Iterator<String> keys=read().keys();while(keys.hasNext())result.add(keys.next());result.sort(String.CASE_INSENSITIVE_ORDER);return result;}
 public List<String> items(String name){List<String> result=new ArrayList<>();JSONArray array=read().optJSONArray(name);if(array!=null)for(int i=0;i<array.length();i++)result.add(array.optString(i));return result;}
 public void save(String name,List<String> items){try{JSONObject all=read();all.put(name,new JSONArray(items));write(all);}catch(JSONException e){throw new IllegalStateException(e);}}
 public void rename(String old,String name,List<String> items){try{JSONObject all=read();if(old!=null)all.remove(old);all.put(name,new JSONArray(items));write(all);}catch(JSONException e){throw new IllegalStateException(e);}}
 public void delete(String name){JSONObject all=read();all.remove(name);write(all);}
 private void write(JSONObject all){if(damaged)throw new IllegalStateException("Las listas guardadas no son válidas; no se sobrescribirán.");preferences.edit().putString("playlists",all.toString()).apply();}
}
