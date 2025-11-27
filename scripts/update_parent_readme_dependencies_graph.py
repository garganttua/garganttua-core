#!/usr/bin/env python3
import os
import xml.etree.ElementTree as ET
import re

# Racine du projet Garganttua-Core
ROOT_DIR = "./"  # à adapter si nécessaire
README_FILE = os.path.join(os.path.dirname(__file__), "..", "README.md")
MODULE_PREFIX = "com.garganttua.core"

# Balisage dans le README
START_TAG = "<!-- AUTO-GENERATED-DEPENDENCIES-GRAPH-START -->"
END_TAG = "<!-- AUTO-GENERATED-DEPENDENCIES-GRAPH-STOP -->"

def parse_pom(pom_path):
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    
    artifact_id_el = root.find("m:artifactId", ns)
    artifact_id = artifact_id_el.text if artifact_id_el is not None else None
    
    dependencies = []
    deps_el = root.find("m:dependencies", ns)
    if deps_el is not None:
        for dep in deps_el.findall("m:dependency", ns):
            group_id = dep.find("m:groupId", ns)
            artifact = dep.find("m:artifactId", ns)
            if group_id is not None and artifact is not None:
                if group_id.text.startswith(MODULE_PREFIX):
                    dependencies.append(artifact.text)
    return artifact_id, dependencies

def find_all_poms(root_dir):
    poms = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        if "pom.xml" in filenames:
            poms.append(os.path.join(dirpath, "pom.xml"))
    return poms

def build_mermaid_graph(poms):
    nodes = set()
    edges = []

    for pom in poms:
        module, deps = parse_pom(pom)
        if module:
            nodes.add(module)
            for dep in deps:
                edges.append((module, dep))
    
    lines = ["```mermaid", "graph TD"]
    for n in sorted(nodes):
        lines.append(f'    {n}["{n}"]')
    lines.append("")
    for src, tgt in edges:
        lines.append(f'    {src} --> {tgt}')
    lines.append("```")
    return "\n".join(lines)

def update_readme(readme_file, graph_content):
    with open(readme_file, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = re.compile(
        f"{START_TAG}.*?{END_TAG}", re.DOTALL
    )

    replacement = f"{START_TAG}\n{graph_content}\n{END_TAG}"
    
    if re.search(pattern, content):
        new_content = re.sub(pattern, replacement, content)
    else:
        # Si les balises n'existent pas, on les ajoute à la fin
        new_content = content + "\n\n" + replacement

    with open(readme_file, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"README.md updated with internal dependency graph between {START_TAG} and {END_TAG}")

def main():
    poms = find_all_poms(ROOT_DIR)
    graph = build_mermaid_graph(poms)
    update_readme(README_FILE, graph)

if __name__ == "__main__":
    main()
