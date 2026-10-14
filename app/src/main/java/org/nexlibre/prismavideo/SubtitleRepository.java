package org.nexlibre.prismavideo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.Locale;

/** Sidecar discovery only inside a directory explicitly authorized through SAF. */
public final class SubtitleRepository {
 private final ContentResolver resolver;
 public SubtitleRepository(ContentResolver resolver){this.resolver=resolver;}
 public Uri associated(Uri tree,String title){
  String base=title;int dot=base.lastIndexOf('.');if(dot>0)base=base.substring(0,dot);base=base.toLowerCase(Locale.ROOT);
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri vtt=null;
  try(Cursor cursor=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
   if(cursor!=null)while(cursor.moveToNext()){String name=cursor.getString(1);if(name==null)continue;name=name.toLowerCase(Locale.ROOT);if(name.equals(base+".srt"))return DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));if(name.equals(base+".vtt"))vtt=DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));}
  }
  return vtt;
 }
}
