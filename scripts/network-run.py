#!/usr/bin/env python3
"""Use the managed environment's proxy and CA for Java tools, without logging credentials."""
import os
import subprocess
import sys
from urllib.parse import urlparse

env = os.environ.copy()
opts = []
for scheme in ('http', 'https'):
    proxy = env.get(scheme.upper() + '_PROXY') or env.get(scheme + '_proxy')
    if proxy:
        url = urlparse(proxy)
        opts += [f'-D{scheme}.proxyHost={url.hostname}', f'-D{scheme}.proxyPort={url.port or 80}']
ca = '/etc/ssl/certs/java/cacerts'
if os.path.isfile(ca):
    opts += [f'-Djavax.net.ssl.trustStore={ca}']
env['JAVA_TOOL_OPTIONS'] = env.get('JAVA_TOOL_OPTIONS', '') + ' ' + ' '.join(opts)
sys.exit(subprocess.call(sys.argv[1:], env=env))
