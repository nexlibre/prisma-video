#!/usr/bin/env python3
"""Install the released beta, seed real legacy prefs, upgrade, and compare values."""
import json
import os
import subprocess
import sys
import xml.etree.ElementTree as ET

package = 'org.nexlibre.prismavideo'
uri = 'content://media/external/video/media/7'
fixtures = {
 'settings': {'night': True, 'accent': 1, 'playlists': json.dumps({'Conservar': [uri]}), 'favorites': {uri}, 'excluded': {'Movies/Private'}, 'markers:'+uri: '[{"position":1000,"name":"Prueba"}]', 'subtitle:'+uri: 'content://test/subtitle/1'},
 'history': {'position:'+uri: 1234, 'items': '[{"uri":"'+uri+'","title":"Prueba","folder":"Movies","duration":2000}]'}
}
def adb(*args, **kwargs):
 return subprocess.run(['adb', *args], check=True, **kwargs)
def encode(values):
 root=ET.Element('map')
 for name,value in values.items():
  if isinstance(value,bool): ET.SubElement(root,'boolean',name=name,value=str(value).lower())
  elif isinstance(value,int): ET.SubElement(root,'long' if name.startswith('position:') else 'int',name=name,value=str(value))
  elif isinstance(value,set):
   child=ET.SubElement(root,'set',name=name)
   for item in sorted(value): ET.SubElement(child,'string').text=item
  else: ET.SubElement(root,'string',name=name).text=value
 return ET.tostring(root,encoding='utf-8',xml_declaration=True)
def decode(data):
 result={}
 for child in ET.fromstring(data):
  if child.tag=='set': value={entry.text for entry in child}
  elif child.tag=='boolean': value=child.get('value')=='true'
  elif child.tag in ('int','long'): value=int(child.get('value'))
  else: value=child.text or ''
  result[child.get('name')]=value
 return result
old,new=sys.argv[1:3]
subprocess.run(['apksigner','verify',old],check=True)
subprocess.run(['apksigner','verify',new],check=True)
def certificate(path):
 output=subprocess.check_output(['apksigner','verify','--print-certs',path],text=True)
 return next(line.split(': ',1)[1] for line in output.splitlines() if line.startswith('Signer #1 certificate SHA-256 digest:'))
assert certificate(old)==certificate(new), 'Signing certificate changed'
adb('install','-r',old)
adb('shell','am','force-stop',package)
for filename,values in fixtures.items():
 adb('shell','run-as',package,'sh','-c',f"'mkdir -p shared_prefs && cat > shared_prefs/{filename}.xml'",input=encode(values))
adb('install','-r',new)
adb('shell','am','start','-W','-n',package+'/.MainActivity')
adb('shell','am','force-stop',package)
for filename,values in fixtures.items():
 output=subprocess.check_output(['adb','shell','run-as',package,'cat','shared_prefs/'+filename+'.xml'])
 actual=decode(output)
 for key,value in values.items(): assert actual.get(key)==value, f'Lost legacy value: {filename}/{key}'
print('PASS: released 2.0.1 -> current APK; identical certificate; playlists, favorites, exclusions, markers, subtitle references, history and positions preserved')
