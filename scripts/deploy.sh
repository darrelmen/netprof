#!/bin/bash
#
# DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
# and their contractors; 2016 - 2018. Other request for this document shall
# be referred to DLIFLC.
#
# WARNING: This document may contain technical data whose export is restricted
# by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
# Transfer of this data by any means to a non-US person who is not eligible to
# obtain export-controlled data is prohibited. By accepting this data, the consignee
# agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
# unclassified, limited distribution documents, destroy by any method that will
# prevent disclosure of the contents or reconstruction of the document.
#
# This material is based upon work supported under Air Force Contract No.
# FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
# or recommendations expressed in this material are those of the author(s) and
# do not necessarily reflect the views of the U.S. Air Force.
#
# © 2016 - 2018 Massachusetts Institute of Technology.
#
# The software/firmware is provided to you on an As-Is basis
#
# Delivered to the US Government with Unlimited Rights, as defined in DFARS
# Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
# U.S. Government rights in this work are defined by DFARS 252.227-7013 or
# DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
# authorized by the U.S. Government may violate any copyrights that exist in this work.
#

# this is taken from domino - thanks Ray!
# GWVF 08/01/2018

if [ -z "$2" ]; then
usage
fi

if [ -e /opt/netprof/config/.shell_settings ]; then
   source /opt/netprof/config/.shell_settings
else
    echo "settings file /opt/netprof/config/.shell_settings is required"
    exit 1
fi
   
if [ -z ${art_user+x} ] || [ -z ${art_pass+x} ] || [ -z ${art_base+x} ]; then
    echo "art_user, art_pass, and art_base are required";
    exit 1
fi
			 
localFilePath="$1"
artifactoryHome="https://kws-bugs.ll.mit.edu/artifactory"
targetFile="$art_base/$2"

if [ ! -f "$localFilePath" ]; then
echo "ERROR: local file $localFilePath does not exist!"
    exit 1
fi

which md5sum || exit $?
which sha1sum || exit $?

md5Value="`md5sum "$localFilePath"`"
md5Value="${md5Value:0:32}"
sha1Value="`sha1sum "$localFilePath"`"
sha1Value="${sha1Value:0:40}"
fileName="`basename "$localFilePath"`"

echo $md5Value $sha1Value $localFilePath

echo "INFO: Uploading $localFilePath to $targetFile"
curl -k -i -X PUT -u $art_user:$art_pass \
 -H "X-Checksum-Md5: $md5Value" \
 -H "X-Checksum-Sha1: $sha1Value" \
 -T "$localFilePath" \
 "$artifactoryHome/$targetFile"


