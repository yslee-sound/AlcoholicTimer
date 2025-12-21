#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Reorganize language strings.xml files to match English structure
"""

import xml.etree.ElementTree as ET
import sys
from pathlib import Path

def parse_xml(file_path):
    """Parse XML file and return root element"""
    tree = ET.parse(file_path, parser=ET.XMLParser(encoding='utf-8'))
    return tree.getroot()

def extract_strings_dict(root):
    """Extract all string and string-array elements into dictionaries"""
    strings = {}
    arrays = {}

    for elem in root:
        if elem.tag == 'string' and 'name' in elem.attrib:
            strings[elem.attrib['name']] = elem
        elif elem.tag == 'string-array' and 'name' in elem.attrib:
            arrays[elem.attrib['name']] = elem

    return strings, arrays

def extract_structure(file_path):
    """Extract the structure (order and comments) from reference file"""
    structure = []

    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    in_resources = False
    for line in lines:
        stripped = line.strip()

        if '<resources>' in stripped:
            in_resources = True
            continue
        elif '</resources>' in stripped:
            break

        if not in_resources:
            continue

        # Extract comments
        if stripped.startswith('<!--') and stripped.endswith('-->'):
            comment = stripped[4:-3].strip()
            structure.append(('comment', comment))
        # Extract string names
        elif stripped.startswith('<string name="'):
            name_start = stripped.find('name="') + 6
            name_end = stripped.find('"', name_start)
            if name_end > name_start:
                name = stripped[name_start:name_end]
                structure.append(('string', name))
        # Extract string-array names
        elif stripped.startswith('<string-array name="'):
            name_start = stripped.find('name="') + 6
            name_end = stripped.find('"', name_start)
            if name_end > name_start:
                name = stripped[name_start:name_end]
                structure.append(('array', name))

    return structure

def write_reorganized_xml(output_path, structure, strings, arrays):
    """Write reorganized XML file"""
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')

        for item_type, content in structure:
            if item_type == 'comment':
                f.write(f'    <!-- {content} -->\n')
            elif item_type == 'string' and content in strings:
                elem = strings[content]
                # Build string element
                attrs = ' '.join(f'{k}="{v}"' for k, v in elem.attrib.items())
                text = elem.text if elem.text else ''
                # Escape special characters
                text = text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
                # But unescape if already escaped
                if '&amp;' in str(ET.tostring(elem, encoding='unicode')):
                    text = ET.tostring(elem, encoding='unicode', method='html')
                    text = text.split('>', 1)[1].rsplit('<', 1)[0]

                f.write(f'    <string {attrs}>{text}</string>\n')
            elif item_type == 'array' and content in arrays:
                elem = arrays[content]
                # Write string-array with all items
                f.write(f'    <string-array name="{elem.attrib["name"]}">\n')
                for item in elem.findall('item'):
                    item_text = item.text if item.text else ''
                    f.write(f'        <item>{item_text}</item>\n')
                f.write('    </string-array>\n')

        f.write('\n</resources>\n')

def main():
    if len(sys.argv) != 4:
        print("Usage: reorganize_strings.py <english_xml> <target_xml> <output_xml>")
        sys.exit(1)

    english_path = Path(sys.argv[1])
    target_path = Path(sys.argv[2])
    output_path = Path(sys.argv[3])

    # Extract structure from English file
    structure = extract_structure(english_path)

    # Parse target language file
    target_root = parse_xml(target_path)
    strings, arrays = extract_strings_dict(target_root)

    # Write reorganized file
    write_reorganized_xml(output_path, structure, strings, arrays)

    print(f"Successfully reorganized: {output_path}")

if __name__ == '__main__':
    main()

