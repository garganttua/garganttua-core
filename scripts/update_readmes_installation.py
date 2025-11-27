import xml.etree.ElementTree as ET
import os
import re
from pathlib import Path

START = "<!-- AUTO-GENERATED-START -->"
END = "<!-- AUTO-GENERATED-END -->"


def load_pom(path):
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    tree = ET.parse(path)
    root = tree.getroot()
    return root, ns


def extract_coordinates(pom_path):
    """Retourne groupId et artifactId du pom."""
    root, ns = load_pom(pom_path)

    artifact = root.find("m:artifactId", ns).text.strip()

    group = root.find("m:groupId", ns)
    if group is not None:
        group = group.text.strip()
    else:
        # Héritage depuis pom parent
        parent = root.find("m:parent", ns)
        group = parent.find("m:groupId", ns).text.strip()

    return group, artifact


def extract_parent_version(parent_pom_path):
    root, ns = load_pom(parent_pom_path)
    version_node = root.find("m:version", ns)
    return version_node.text.strip() if version_node is not None else None


def extract_parent_properties(parent_pom_path):
    root, ns = load_pom(parent_pom_path)
    props = root.find("m:properties", ns)
    prop_map = {}

    if props is not None:
        for child in props:
            tag = child.tag.split("}")[1]
            prop_map[tag] = child.text.strip()

    return prop_map


def resolve_version_if_needed(dep, properties):
    version = dep.get("version")

    if version and version.startswith("${") and version.endswith("}"):
        key = version[2:-1]
        return properties.get(key, version)

    return version


def parse_pom_dependencies(pom_path, parent_properties):
    root, ns = load_pom(pom_path)

    deps = []
    for dep in root.findall("m:dependencies/m:dependency", ns):
        group = dep.find("m:groupId", ns).text
        artifact = dep.find("m:artifactId", ns).text
        version_tag = dep.find("m:version", ns)
        scope_tag = dep.find("m:scope", ns)

        version = None
        if version_tag is not None:
            version = version_tag.text.strip()
            version = resolve_version_if_needed({"version": version}, parent_properties)

        scope = scope_tag.text.strip() if scope_tag is not None else None

        item = f"{group}:{artifact}"
        if version:
            item += f":{version}"
        if scope:
            item += f":{scope}"

        deps.append(item)

    return deps


def build_maven_installation(group, artifact, version):
    return (
        "```xml\n"
        "<dependency>\n"
        f"    <groupId>{group}</groupId>\n"
        f"    <artifactId>{artifact}</artifactId>\n"
        f"    <version>{version}</version>\n"
        "</dependency>\n"
        "```\n\n"
    )


def build_markdown(version, deps, group, artifact):
    md = []
    md.append("### Installation with Maven\n")
    md.append(build_maven_installation(group, artifact, version))

    md.append("### Actual version\n")
    md.append(f"{version}\n\n")

    md.append("### Dependencies\n")
    for d in deps:
        md.append(f" - `{d}`\n")

    return "".join(md)


def update_readme(readme_path, md_block):
    with open(readme_path, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = re.compile(rf"{START}.*?{END}", re.DOTALL)
    replacement = f"{START}\n{md_block}\n{END}"

    new_content = re.sub(pattern, replacement, content)

    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"✔ Updated {readme_path}")


def find_modules(pom_path):
    root, ns = load_pom(pom_path)
    module_nodes = root.findall("m:modules/m:module", ns)
    return [m.text.strip() for m in module_nodes]


def process_module(module_path, parent_version, parent_props):
    pom_path = os.path.join(module_path, "pom.xml")
    readme_path = os.path.join(module_path, "README.md")

    if not os.path.exists(pom_path) or not os.path.exists(readme_path):
        return

    deps = parse_pom_dependencies(pom_path, parent_props)
    group, artifact = extract_coordinates(pom_path)

    md = build_markdown(parent_version, deps, group, artifact)
    update_readme(readme_path, md)

    for submodule in find_modules(pom_path):
        process_module(os.path.join(module_path, submodule), parent_version, parent_props)


def main():
    workdir = Path(os.getcwd()).resolve()
    script_dir = Path(__file__).resolve().parent
    parent_dir = script_dir.parent if script_dir.name == "scripts" else script_dir

    pom_parent = parent_dir / "pom.xml"
    readme_parent = parent_dir / "README.md"

    if not pom_parent.exists():
        raise FileNotFoundError("Parent pom.xml not found")

    parent_version = extract_parent_version(pom_parent)
    parent_props = extract_parent_properties(pom_parent)
    group, artifact = extract_coordinates(pom_parent)

    if readme_parent.exists():
        deps = parse_pom_dependencies(pom_parent, parent_props)
        md = build_markdown(parent_version, deps, group, artifact)
        update_readme(readme_parent, md)

    for module in find_modules(pom_parent):
        process_module(parent_dir / module, parent_version, parent_props)


if __name__ == "__main__":
    main()