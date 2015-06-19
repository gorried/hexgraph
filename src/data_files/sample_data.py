#! /usr/bin/env python

"""
Daniel Gorrie
Large dataset sampler
"""

import random
import os
from os import listdir
from os.path import isfile, join

# Constants
INPUT_FILE = 'train.features'
INPUT_FILE_SIZE = 8352136
OUTPUT_FILE = 'train_small.features'
SAMPLE_SIZE = 110000
INPUT_LABEL_DIR = 'labels/'
OUTPUT_LABEL_DIR = 'labels_small/'



def main():
    random.seed()

    # Generate array of SAMPLE_SIZE random integers in range [0, INPUT_FILE.length)
    # Iterate over the input file grabbing the

    indices = dict.fromkeys([random.randint(0, INPUT_FILE_SIZE) for _ in xrange(SAMPLE_SIZE)])
    while len(indices) < SAMPLE_SIZE:
        indices[random.randint(0, INPUT_FILE_SIZE)] = 0


    # Grab the proper training data
    with open(OUTPUT_FILE, 'w') as out:
        with open(INPUT_FILE, 'r') as f:
            line_count = 0
            for line in f:
                if line_count in indices:
                    # append the line to the output file
                    out.write(line)
                line_count += 1

    # Grab the label files
    label_files = [ f for f in listdir(INPUT_LABEL_DIR) if isfile(join(INPUT_LABEL_DIR,f)) ]

    # make a new directory
    d = os.path.dirname(OUTPUT_LABEL_DIR)
    if not os.path.exists(d):
        os.makedirs(d)

    # put versions of all the label files in the output directory

    for label_file in label_files:
        with open(INPUT_LABEL_DIR + label_file, 'r') as f:
            with open (OUTPUT_LABEL_DIR + label_file, 'w') as out:
                line_count = 0
                for line in f:
                    if line_count in indices:
                        # append the line to the output file
                        out.write(line)
                    line_count += 1





if __name__ == '__main__':
    main()



