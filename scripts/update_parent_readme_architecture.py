import os
import re
import xml.etree.ElementTree as ET

def extract_description_from_pom(pom_path: str) -> str:
    """
    Lit le pom.xml et extrait le contenu de la balise <description>.
    """
    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = {'mvn': root.tag.split('}')[0].strip('{')} if '}' in root.tag else {}
        desc_elem = root.find('mvn:description', ns) if ns else root.find('description')
        if desc_elem is not None and desc_elem.text:
            return desc_elem.text.strip().replace('\n', ' ')
    except ET.ParseError:
        print(f"Warning: Failed to parse {pom_path}")
    return ""

def format_module_name(name: str, depth: int) -> str:
    """
    Formate le nom du module avec une indentation en Markdown pour la hiérarchie.
    """
    if depth == 0:
        return name
    else:
        prefix = "|    " * (depth - 1) + "|- "
        # échapper les | pour Markdown
        prefix = prefix.replace("|", "\\|")
        return prefix + name

def generate_modules_table(root_dir: str) -> str:
    """
    Parcourt les modules et crée une table Markdown avec hiérarchie.
    """
    table_lines = []
    table_lines.append("| Module | Description |")
    table_lines.append("|:--|:--|")

    def walk_dir(dir_path: str, depth: int = 0):
        if "pom.xml" in os.listdir(dir_path):
            module_name = os.path.basename(dir_path)
            pom_path = os.path.join(dir_path, "pom.xml")
            description = extract_description_from_pom(pom_path)
            rel_path = os.path.relpath(dir_path, root_dir).replace(os.sep, "/")
            readme_link = f"./{rel_path}/README.md"
            display_name = format_module_name(f"[**{module_name}**]({readme_link})", depth)
            table_lines.append(f"| {display_name} | {description} |")
        
        # Parcours les sous-modules
        for subdir in sorted(os.listdir(dir_path)):
            sub_path = os.path.join(dir_path, subdir)
            if os.path.isdir(sub_path):
                walk_dir(sub_path, depth + 1)

    walk_dir(root_dir)
    return "\n".join(table_lines)

def update_readme(readme_path: str, new_table: str):
    """
    Remplace le contenu entre les balises AUTO-GENERATED-ARCHITECTURE dans le README.
    """
    if not os.path.isfile(readme_path):
        print(f"[WARN] README not found at {readme_path}, skipping update.")
        return

    start_tag = "<!-- AUTO-GENERATED-ARCHITECTURE-START -->"
    end_tag = "<!-- AUTO-GENERATED-ARCHITECTURE-STOP -->"

    with open(readme_path, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = re.compile(
        rf"({re.escape(start_tag)})(.*?)(\s*{re.escape(end_tag)})",
        re.DOTALL
    )
    new_content = pattern.sub(f"\\1\n{new_table}\n\\3", content)

    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(new_content)
    print(f"README updated at {readme_path}")

if __name__ == "__main__":
    # Calcul du chemin racine du projet peu importe d'où on lance le script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))
    readme_file = os.path.join(project_root, "README.md")

    table_markdown = generate_modules_table(project_root)
    update_readme(readme_file, table_markdown)