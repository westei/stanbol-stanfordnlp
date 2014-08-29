#!/bin/sh -e -o pipefail

RELEASE_DATE_v310=2013-11-12
RELEASE_DATE_v311=2014-01-04
#Archive names
WORD_SEGMENTOR_ARCHIVE=stanford-segmenter-${RELEASE_DATE_v311}.zip
POS_MODEL_ARCHIVE=stanford-postagger-full-${RELEASE_DATE_v311}.zip
NER_CHINESE_ARCHIVE=stanford-chinese-corenlp-${RELEASE_DATE_v310}-models.jar
#German NER does not exist for newer releases
#NER_GERMAN_ARCHIVE=stanford-ner-2012-05-22-german.tgz

#the list of the used POS models
POS_MODELS=(\
    arabic \
    chinese-distsim \
    french \
#    german-fast \
    )

if [ ! -d src/main/resources/models ]; then
    mkdir src/main/resources/models
fi


if [ ! -d downloads ]; then
    echo "create downloads directory"
    mkdir downloads
fi

cd downloads

# Download wordsegmentor
if [ ! -f ${WORD_SEGMENTOR_ARCHIVE} ]; then
    echo "download POS Models (version: ${WORD_SEGMENTOR_ARCHIVE})"
    wget http://nlp.stanford.edu/software/${WORD_SEGMENTOR_ARCHIVE}
fi

segmentor_version_path="${WORD_SEGMENTOR_ARCHIVE%.*}"
unzip -o ${WORD_SEGMENTOR_ARCHIVE}

if [ ! -d ../src/main/resources/models/seg ]; then
    mkdir ../src/main/resources/models/seg
else
    rm -rf ../src/main/resources/models/seg
    mkdir ../src/main/resources/models/seg
fi

mv ${segmentor_version_path}/data/* ../src/main/resources/models/seg
rm -rf ${segmentor_version_path}


# Download POS Tagger data
if [ ! -f ${POS_MODEL_ARCHIVE} ]; then
    echo "download POS Models (version: ${POS_MODEL_ARCHIVE})"
    wget http://nlp.stanford.edu/software/${POS_MODEL_ARCHIVE}
fi

# Extract the used models
pos_model_path="${POS_MODEL_ARCHIVE%.*}"
for pos_model in "${POS_MODELS[@]}"; do
    unzip -o -j ${POS_MODEL_ARCHIVE} ${pos_model_path}/models/${pos_model}.tagger* -d ../src/main/resources/models
done

# Download German NER model
#if [ ! -f ${NER_GERMAN_ARCHIVE} ]
#then
#    wget http://nlp.stanford.edu/software/${NER_GERMAN_ARCHIVE}
#fi

#german_model_path="${NER_GERMAN_ARCHIVE%.*}"
#tar zxvf ${NER_GERMAN_ARCHIVE} -C ../src/main/resources/models --include "hgc_175m_600*"


# Download Chinese NER
if [ ! -f ${NER_CHINESE_ARCHIVE} ]; then
    wget http://nlp.stanford.edu/software/${NER_CHINESE_ARCHIVE}
fi

# chinese_model_path="${NER_CHINESE_ARCHIVE%.*}"
unzip -o -j ${NER_CHINESE_ARCHIVE} edu/stanford/nlp/models/ner/chinese.misc.distsim* -d ../src/main/resources/models


cd ..
