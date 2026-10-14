package org.nexlibre.prismavideo;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RepositoryTest {
 @Test public void readsLegacyListsAndPreservesOtherData(){
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();SharedPreferences prefs=context.getSharedPreferences("repository-tests",Context.MODE_PRIVATE);prefs.edit().clear().putString("playlists","{\"Vieja\":[\"content://media/external/video/media/7\",\"content://media/external/video/media/8\"]}").putStringSet("favorites",new java.util.HashSet<>(Arrays.asList("content://media/external/video/media/7"))).putBoolean("night",true).commit();
  PlaylistRepository repository=new PlaylistRepository(prefs);assertEquals(2,repository.items("Vieja").size());repository.rename("Vieja","Nueva",repository.items("Vieja"));assertEquals(Arrays.asList("Nueva"),repository.names());assertTrue(prefs.getBoolean("night",false));assertTrue(prefs.getStringSet("favorites",java.util.Collections.emptySet()).contains("content://media/external/video/media/7"));repository.save("Nueva",Arrays.asList("a","b","c"));repository.delete("Nueva");assertTrue(repository.names().isEmpty());prefs.edit().clear().commit();
 }
 @Test public void damagedListsAreNotOverwritten(){
  SharedPreferences prefs=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("repository-tests-damaged",Context.MODE_PRIVATE);prefs.edit().clear().putString("playlists","damaged-json").commit();PlaylistRepository repository=new PlaylistRepository(prefs);assertTrue(repository.names().isEmpty());try{repository.save("Nueva",Arrays.asList("a"));fail("Damaged data overwritten");}catch(IllegalStateException expected){}assertEquals("damaged-json",prefs.getString("playlists",""));prefs.edit().clear().commit();
 }
 @Test public void folderNormalizationPreservesBoundaries(){assertEquals("Movies/Private",LibraryRepository.normalize("/storage/emulated/0/Movies/Private/"));assertEquals("Movies/Private",LibraryRepository.normalize("sdcard/Movies/Private"));assertEquals("Movies2",LibraryRepository.normalize("Movies2/"));}
}
