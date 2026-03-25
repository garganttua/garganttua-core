#!/usr/bin/env python3
"""Generate draw.io architecture diagrams for all garganttua-core modules."""
import os

BASE = os.path.dirname(os.path.abspath(__file__))

# ── Styles ──────────────────────────────────────────────────────────────────
S = {
    "iface":    "rounded=0;whiteSpace=wrap;fillColor=#dae8fc;strokeColor=#6c8ebf;fontStyle=1;fontSize=12;",
    "class":    "rounded=0;whiteSpace=wrap;fillColor=#f5f5f5;strokeColor=#666666;fontSize=12;",
    "abstract": "rounded=0;whiteSpace=wrap;fillColor=#e1d5e7;strokeColor=#9673a6;fontStyle=3;fontSize=12;",
    "enum":     "rounded=0;whiteSpace=wrap;fillColor=#d5e8d4;strokeColor=#82b366;fontSize=12;",
    "annot":    "rounded=0;whiteSpace=wrap;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=12;",
    "record":   "rounded=0;whiteSpace=wrap;fillColor=#f0f0ff;strokeColor=#9999cc;fontSize=12;",
    "title":    "text;fontSize=20;fontStyle=1;align=center;verticalAlign=middle;",
    "subtitle": "text;fontSize=11;align=center;fontColor=#888888;",
    "note":     "shape=note;whiteSpace=wrap;fillColor=#ffffcc;strokeColor=#cccc00;fontSize=10;align=left;verticalAlign=top;spacingLeft=4;",
    "group":    "rounded=1;whiteSpace=wrap;fillColor=#f8f8f8;strokeColor=#cccccc;fontSize=12;fontStyle=1;dashed=1;verticalAlign=top;spacingTop=5;",
}
E = {
    "impl":    "endArrow=block;endFill=0;dashed=1;strokeColor=#6c8ebf;",
    "extends": "endArrow=block;endFill=0;strokeColor=#666666;",
    "uses":    "endArrow=open;dashed=1;strokeColor=#999999;",
    "creates": "endArrow=open;dashed=1;strokeColor=#d79b00;",
    "flow":    "endArrow=classic;endFill=1;strokeColor=#0066cc;strokeWidth=2;",
    "dep":     "endArrow=open;dashed=1;strokeColor=#bbbbbb;",
}
LAYER = {
    "foundation":     ("#d5e8d4", "#82b366"),
    "infrastructure": ("#dae8fc", "#6c8ebf"),
    "framework":      ("#e1d5e7", "#9673a6"),
    "application":    ("#fff2cc", "#d6b656"),
    "build":          ("#f5f5f5", "#999999"),
    "integration":    ("#ffe6cc", "#d79b00"),
    "aot":            ("#f8cecc", "#b85450"),
}

# ── Helpers ─────────────────────────────────────────────────────────────────
def esc(s):
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace('"',"&quot;")

def cell(id, val, style, x, y, w, h, parent="1"):
    return f'        <mxCell id="{id}" value="{esc(val)}" style="{style}" vertex="1" parent="{parent}"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>'

def edge(id, src, tgt, style, parent="1"):
    return f'        <mxCell id="{id}" style="{style}" edge="1" source="{src}" target="{tgt}" parent="{parent}"><mxGeometry relative="1" as="geometry"/></mxCell>'

def container(id, val, fill, stroke, x, y, w, h, parent="1"):
    st = f"swimlane;startSize=28;rounded=1;fillColor={fill};strokeColor={stroke};fontStyle=1;fontSize=14;collapsible=0;"
    return f'        <mxCell id="{id}" value="{esc(val)}" style="{st}" vertex="1" parent="{parent}"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>'

def mod_box(id, val, stroke, x, y, w, h, parent):
    st = f"rounded=1;whiteSpace=wrap;fillColor=#FFFFFF;strokeColor={stroke};fontStyle=1;fontSize=11;"
    return f'        <mxCell id="{id}" value="{esc(val)}" style="{st}" vertex="1" parent="{parent}"><mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>'

def wrap(name, pw, ph, lines):
    c = "\n".join(lines)
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<mxfile host="app.diagrams.net">
  <diagram id="main" name="{esc(name)}">
    <mxGraphModel dx="1422" dy="762" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="{pw}" pageHeight="{ph}" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
{c}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>'''

def write_file(fname, content):
    with open(os.path.join(BASE, fname), "w") as f:
        f.write(content)
    print(f"  {fname}")

def legend(lines, x, y):
    items = [("Interface","iface"),("Class","class"),("Abstract","abstract"),
             ("Enum","enum"),("Annotation","annot"),("Record","record")]
    for i,(label,t) in enumerate(items):
        lines.append(cell(f"lg{i}", label, S[t], x, y+i*24, 90, 20))

def gen_module(name, title, pw, ph, elems, arrows, notes=None, groups=None):
    """Generate a module diagram. elems: [(id,label,x,y,w,h,type)] arrows: [(src,tgt,type)]"""
    L = []
    L.append(cell("title", title, S["title"], pw//2-250, 8, 500, 30))
    if groups:
        for gid, glabel, gx, gy, gw, gh in groups:
            L.append(cell(gid, glabel, S["group"], gx, gy, gw, gh))
    for id, label, x, y, w, h, t in elems:
        L.append(cell(id, label, S[t], x, y, w, h))
    for i,(src,tgt,t) in enumerate(arrows):
        L.append(edge(f"e{i}", src, tgt, E[t]))
    if notes:
        for nid, txt, nx, ny, nw, nh in notes:
            L.append(cell(nid, txt, S["note"], nx, ny, nw, nh))
    legend(L, pw-120, ph-170)
    write_file(f"garganttua-{name}.drawio", wrap(title, pw, ph, L))


# ═══════════════════════════════════════════════════════════════════════════
#  GLOBAL ARCHITECTURE DIAGRAM
# ═══════════════════════════════════════════════════════════════════════════
def gen_global():
    L = []
    pw, ph = 1500, 1200
    L.append(cell("title", "Garganttua Core - Architecture Modulaire", S["title"], 300, 10, 900, 35))

    # ── Foundation ──
    fill, stroke = LAYER["foundation"]
    cy = 1020; ch = 120
    L.append(container("lf", "Foundation", fill, stroke, 40, cy, 1420, ch))
    mods = [("commons","commons",20,38,180,50),("dsl","dsl",220,38,100,50),
            ("supply","supply",340,38,110,50),("lifecycle","lifecycle",470,38,130,50),
            ("mutex","mutex",620,38,110,50)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "lf"))

    # ── Infrastructure ──
    fill, stroke = LAYER["infrastructure"]
    cy = 860; ch = 120
    L.append(container("li", "Infrastructure", fill, stroke, 40, cy, 1420, ch))
    mods = [("reflection","reflection",20,38,160,50),("rtrefl","runtime-reflection",200,38,210,50),
            ("condition","condition",430,38,140,50),("execution","execution",590,38,140,50),
            ("crypto","crypto",750,38,110,50),("configuration","configuration",880,38,170,50)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "li"))

    # ── Framework ──
    fill, stroke = LAYER["framework"]
    cy = 700; ch = 120
    L.append(container("lfr", "Framework", fill, stroke, 40, cy, 1420, ch))
    mods = [("injection","injection",20,38,150,50),("runtime","runtime",190,38,130,50),
            ("mapper","mapper",340,38,120,50),("expression","expression",480,38,160,50),
            ("bootstrap","bootstrap",660,38,150,50)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "lfr"))

    # ── Application ──
    fill, stroke = LAYER["application"]
    cy = 540; ch = 120
    L.append(container("la", "Application", fill, stroke, 40, cy, 1420, ch))
    mods = [("script","script",20,38,140,50),("console","console",180,38,140,50),
            ("workflow","workflow",340,38,150,50)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "la"))

    # ── Build Tools ──
    fill, stroke = LAYER["build"]
    cy = 390; ch = 110
    L.append(container("lb", "Build Tools", fill, stroke, 40, cy, 1420, ch))
    mods = [("native","native",20,35,120,45),("nativeplugin","native-image-maven-plugin",160,35,260,45),
            ("annotproc","annotation-processor",440,35,220,45),("scriptplugin","script-maven-plugin",680,35,230,45)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "lb"))

    # ── Integration ──
    fill, stroke = LAYER["integration"]
    cy = 280; ch = 75
    L.append(container("lin", "Integration (Bindings)", fill, stroke, 40, cy, 1420, ch))
    mods = [("spring","spring",20,30,150,30),("reflections","reflections",190,30,170,30)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "lin"))

    # ── AOT ──
    fill, stroke = LAYER["aot"]
    cy = 160; ch = 85
    L.append(container("lao", "AOT (Work in Progress)", fill, stroke, 40, cy, 1420, ch))
    mods = [("aotcomm","aot-commons",20,32,160,35),("aotrefl","aot-reflection",200,32,170,35),
            ("aotscan","aot-annotation-scanner",390,32,220,35),("aotproc","aot-annotation-processor",630,32,230,35),
            ("aotplug","aot-maven-plugin",880,32,190,35)]
    for mid,ml,mx,my,mw,mh in mods:
        L.append(mod_box(f"m_{mid}", ml, stroke, mx, my, mw, mh, "lao"))

    # ── Key dependency arrows ──
    deps = [
        ("m_script","m_expression","uses"),("m_script","m_runtime","uses"),
        ("m_console","m_script","uses"),
        ("m_workflow","m_script","uses"),("m_workflow","m_expression","uses"),
        ("m_expression","m_injection","uses"),
        ("m_injection","m_reflection","uses"),("m_injection","m_lifecycle","uses"),
        ("m_injection","m_dsl","uses"),
        ("m_runtime","m_injection","uses"),("m_runtime","m_execution","uses"),
        ("m_configuration","m_reflection","uses"),("m_configuration","m_dsl","uses"),
        ("m_reflection","m_supply","uses"),
        ("m_bootstrap","m_injection","uses"),
    ]
    for i,(src,tgt,t) in enumerate(deps):
        L.append(edge(f"dep{i}", src, tgt, E[t]))

    # ── Legend ──
    lx, ly = 1200, 560
    L.append(cell("leg_title", "Dependency arrows", S["subtitle"], lx, ly, 200, 20))
    L.append(cell("leg_line", "--- uses/depends on", S["subtitle"], lx, ly+22, 200, 18))

    write_file("garganttua-core-architecture.drawio", wrap("Garganttua Core - Architecture", pw, ph, L))


# ═══════════════════════════════════════════════════════════════════════════
#  MODULE DIAGRAMS
# ═══════════════════════════════════════════════════════════════════════════

def gen_commons():
    gen_module("commons", "garganttua-commons - Shared Contracts", 1100, 750,
        [   # Packages shown as groups of key interfaces
            # ── dsl ──
            ("ib","IBuilder<T>",40,80,170,45,"iface"),
            ("ilb","ILinkedBuilder<L,B>",40,140,190,45,"iface"),
            # ── supply ──
            ("isup","ISupplier<T>",280,80,170,45,"iface"),
            ("icsup","IContextualSupplier<T,C>",280,140,230,45,"iface"),
            # ── lifecycle ──
            ("ilf","ILifecycle",560,80,150,45,"iface"),
            # ── reflection ──
            ("irefl","IReflection",40,240,160,45,"iface"),
            ("icls","IClass<T>",220,240,140,45,"iface"),
            ("ifield","IField",380,240,100,45,"iface"),
            ("imeth","IMethod",500,240,110,45,"iface"),
            ("icons","IConstructor",630,240,140,45,"iface"),
            ("iexec","IExecutable",560,310,140,45,"iface"),
            ("oa","ObjectAddress",800,240,150,40,"class"),
            # ── injection ──
            ("iic","IInjectionContext",40,400,190,45,"iface"),
            ("ibf","IBeanFactory<T>",260,400,170,45,"iface"),
            ("bd","BeanDefinition<T>",460,400,180,45,"record"),
            ("br","BeanReference",660,400,150,45,"record"),
            ("bs","BeanStrategy",830,400,130,40,"enum"),
            # ── runtime ──
            ("irt","IRuntime<I,O>",40,500,160,45,"iface"),
            ("irctx","IRuntimeContext",220,500,180,45,"iface"),
            ("irstep","IRuntimeStep",420,500,160,45,"iface"),
            # ── script / expression ──
            ("isc","IScript",40,590,120,45,"iface"),
            ("isf","IScriptFunction",180,590,160,45,"iface"),
            ("iec","IExpressionContext",370,590,200,45,"iface"),
            ("ien","IExpressionNode<R,S>",600,590,210,45,"iface"),
            # ── workflow ──
            ("iwf","IWorkflow",40,670,140,45,"iface"),
            ("wfr","WorkflowResult",200,670,150,45,"record"),
        ],
        [
            ("ilb","ib","extends"),("icsup","isup","extends"),
            ("imeth","iexec","impl"),("icons","iexec","impl"),
        ],
        groups=[
            ("g_dsl","dsl",30,55,210,145),("g_supply","supply",270,55,250,145),
            ("g_life","lifecycle",550,55,170,80),
            ("g_refl","reflection",30,215,930,155),
            ("g_inj","injection",30,375,940,85),
            ("g_rt","runtime",30,475,560,85),
            ("g_script","script + expression",30,565,790,85),
            ("g_wf","workflow",30,645,340,85),
        ])

def gen_dsl():
    gen_module("dsl", "garganttua-dsl - Builder Framework", 700, 400,
        [("ib","IBuilder<T>",50,80,180,50,"iface"),
         ("ilb","ILinkedBuilder<Link, Built>",300,80,250,50,"iface"),
         ("iab","IAutomaticBuilder<T>",50,200,220,50,"iface"),
         ("ialb","IAutomaticLinkedBuilder<L,B>",350,200,280,50,"iface"),
         ("exc","DslException",250,300,160,40,"class")],
        [("ilb","ib","extends"),("iab","ib","extends"),("ialb","ilb","extends")])

def gen_supply():
    gen_module("supply", "garganttua-supply - Supplier Pattern", 700, 380,
        [("isup","ISupplier<T>",50,80,180,50,"iface"),
         ("icsup","IContextualSupplier<T,C>",300,80,260,50,"iface"),
         ("sup","Supplier (utility)",150,200,200,45,"class"),
         ("exc","SupplyException",150,290,180,40,"class")],
        [("icsup","isup","extends"),("sup","isup","uses")])

def gen_lifecycle():
    gen_module("lifecycle", "garganttua-lifecycle - State Management", 600, 350,
        [("ilf","ILifecycle",180,70,200,50,"iface"),
         ("n1","onInit()",80,170,120,35,"note"),
         ("n2","onStart()",220,170,120,35,"note"),
         ("n3","onStop()",360,170,120,35,"note"),
         ("n4","onReload()",120,230,130,35,"note"),
         ("n5","onFlush()",280,230,120,35,"note"),
         ("exc","LifecycleException",180,300,200,35,"class")],
        [("n1","ilf","uses"),("n2","ilf","uses"),("n3","ilf","uses"),
         ("n4","ilf","uses"),("n5","ilf","uses")])

def gen_mutex():
    gen_module("mutex", "garganttua-mutex - Locking Primitives", 600, 350,
        [("imtx","IMutex",200,70,160,50,"iface"),
         ("imf","IMutexFactory",200,170,200,50,"iface"),
         ("mtx","MutexBuilder",200,270,180,45,"class"),
         ("exc","MutexException",420,170,160,40,"class")],
        [("imf","imtx","creates"),("mtx","imf","uses")])

def gen_reflection():
    gen_module("reflection", "garganttua-reflection - Reflection Facade", 1200, 800,
        [   # Facade
            ("irefl","IReflection",450,60,180,50,"iface"),
            ("irp","IReflectionProvider",200,60,210,50,"iface"),
            ("ias","IAnnotationScanner",700,60,200,50,"iface"),
            # Builder
            ("rb","ReflectionBuilder",420,150,200,45,"class"),
            ("cr","CompositeReflection",420,220,210,45,"class"),
            # Core abstractions
            ("icls","IClass<T>",100,300,150,45,"iface"),
            ("ifld","IField",270,300,110,45,"iface"),
            ("imth","IMethod",400,300,120,45,"iface"),
            ("icns","IConstructor",540,300,150,45,"iface"),
            ("iexe","IExecutable",470,370,150,45,"iface"),
            # Accessors
            ("fa","FieldAccessor",80,450,170,45,"class"),
            ("mi","MethodInvoker",280,450,170,45,"class"),
            ("ci","ConstructorInvoker",480,450,190,45,"class"),
            # Resolvers
            ("fr","FieldResolver",80,530,160,45,"class"),
            ("mr","MethodResolver",280,530,170,45,"class"),
            ("cnr","ConstructorResolver",480,530,200,45,"class"),
            # Access managers
            ("fam","FieldAccessManager",80,620,190,40,"class"),
            ("mam","MethodAccessManager",300,620,210,40,"class"),
            ("cam","ConstructorAccessManager",540,620,230,40,"class"),
            # Binders
            ("fb","FieldBinder",800,300,140,45,"class"),
            ("mb","MethodBinder",800,370,150,45,"class"),
            ("cb","ConstructorBinder",800,440,170,45,"class"),
            # ObjectQuery
            ("oq","ObjectQuery",800,540,160,45,"class"),
            ("oa","ObjectAddress",800,620,160,40,"class"),
            # Runtime provider
            ("rrp","RuntimeReflectionProvider",100,160,240,40,"class"),
        ],
        [
            ("cr","irefl","impl"),("cr","irp","uses"),("cr","ias","uses"),
            ("rb","cr","creates"),("rrp","irp","impl"),
            ("imth","iexe","impl"),("icns","iexe","impl"),
            ("fa","ifld","uses"),("mi","imth","uses"),("ci","icns","uses"),
            ("fa","fam","uses"),("mi","mam","uses"),("ci","cam","uses"),
            ("fb","fa","uses"),("mb","mi","uses"),("cb","ci","uses"),
            ("oq","oa","uses"),("oq","fa","uses"),
            ("fr","ifld","uses"),("mr","imth","uses"),("cnr","icns","uses"),
        ],
        notes=[("n_force","force=true parameter\nenables private access\non all access managers",800,160,180,55)])

def gen_runtime_reflection():
    gen_module("runtime-reflection", "garganttua-runtime-reflection - JVM Provider", 600, 350,
        [("irp","IReflectionProvider",180,70,220,50,"iface"),
         ("rrp","RuntimeReflectionProvider",150,180,280,50,"class"),
         ("jdk","JdkClass<T>",200,280,180,45,"class"),
         ("icls","IClass<T>",430,280,150,45,"iface")],
        [("rrp","irp","impl"),("jdk","icls","impl"),("rrp","jdk","creates")])

def gen_condition():
    gen_module("condition", "garganttua-condition - Boolean Condition DSL", 600, 350,
        [("ic","ICondition",200,70,170,50,"iface"),
         ("icb","IConditionBuilder",200,170,200,50,"iface"),
         ("exc","ConditionException",200,280,190,40,"class")],
        [("icb","ic","creates")])

def gen_execution():
    gen_module("execution", "garganttua-execution - Chain of Responsibility", 700, 400,
        [("iex","IExecutor<T>",50,80,170,50,"iface"),
         ("iec","IExecutorChain<T>",280,80,210,50,"iface"),
         ("ifb","IFallBackExecutor<T>",280,190,230,50,"iface"),
         ("exc","ExecutorException",180,300,190,40,"class")],
        [("iec","iex","uses"),("ifb","iex","extends")])

def gen_crypto():
    gen_module("crypto", "garganttua-crypto - Cryptographic Utilities", 600, 300,
        [("cu","CryptoUtils",150,80,200,50,"class"),
         ("hs","HashUtils",150,170,180,45,"class"),
         ("exc","CryptoException",380,80,170,40,"class")],
        [])

def gen_configuration():
    gen_module("configuration", "garganttua-configuration - Multi-Format Config", 1100, 650,
        [   # Sources
            ("ics","IConfigurationSource",50,80,220,45,"iface"),
            ("fcs","FileConfigurationSource",50,160,230,40,"class"),
            ("ccs","ClasspathConfigurationSource",50,210,260,40,"class"),
            ("scs","StringConfigurationSource",50,260,240,40,"class"),
            ("ecs","EnvironmentConfigurationSource",50,310,270,40,"class"),
            # Format
            ("icf","IConfigurationFormat",380,80,210,45,"iface"),
            # Node
            ("icn","IConfigurationNode",650,80,210,45,"iface"),
            # Populator
            ("icp","IConfigurationPopulator",380,200,230,45,"iface"),
            # Builder
            ("cb","ConfigurationBuilder",380,300,220,45,"class"),
            # DI integration
            ("cpp","ConfigurationPropertyProvider",650,200,270,45,"class"),
            ("ipp","IPropertyProvider",650,280,200,45,"iface"),
            # Annotations
            ("ac","@Configurable",380,420,160,35,"annot"),
            ("acp","@ConfigProperty",560,420,170,35,"annot"),
            ("aci","@ConfigIgnore",750,420,150,35,"annot"),
            # Strategies
            ("ms","MappingStrategy",380,510,170,40,"enum"),
        ],
        [
            ("fcs","ics","impl"),("ccs","ics","impl"),("scs","ics","impl"),("ecs","ics","impl"),
            ("icp","ics","uses"),("icp","icf","uses"),("icp","icn","creates"),
            ("cb","icp","creates"),("cpp","ipp","impl"),("cpp","icn","uses"),
        ],
        notes=[("n_fmt","Formats: JSON, YAML,\nXML, TOML, Properties\n(conditional on classpath)",650,370,180,55)])

def gen_injection():
    gen_module("injection", "garganttua-injection - DI Container", 1100, 700,
        [   # Core context
            ("iic","IInjectionContext",80,80,210,50,"iface"),
            ("iicb","IInjectionContextBuilder",80,170,250,45,"iface"),
            ("dcb","DiContextBuilder",80,250,200,45,"class"),
            # Bean management
            ("ibp","IBeanProvider",400,80,180,45,"iface"),
            ("ibf","IBeanFactory<T>",400,170,190,45,"iface"),
            ("ibq","IBeanQuery",620,80,160,45,"iface"),
            ("ibqb","IBeanQueryBuilder",620,170,200,45,"iface"),
            # Bean metadata
            ("bd","BeanDefinition<T>",400,280,190,45,"record"),
            ("bref","BeanReference<T>",620,280,180,45,"record"),
            ("bs","BeanStrategy",830,280,140,40,"enum"),
            # Properties
            ("ipp","IPropertyProvider",80,380,190,45,"iface"),
            ("ips","IPropertySupplier",300,380,180,45,"iface"),
            # Child context
            ("iccf","IDiChildContextFactory",550,380,240,45,"iface"),
            # Resolver
            ("ier","IInjectableElementResolver",80,470,260,45,"iface"),
            # Lifecycle
            ("lc","ILifecycle",400,470,160,45,"iface"),
            # Annotations
            ("ap","@Property",80,570,120,35,"annot"),
            ("apv","@Provider",220,570,120,35,"annot"),
            ("apt","@Prototype",360,570,130,35,"annot"),
            ("af","@Fixed",510,570,100,35,"annot"),
            ("an","@Null",630,570,100,35,"annot"),
        ],
        [
            ("dcb","iicb","impl"),("iicb","iic","creates"),
            ("iic","ibp","uses"),("iic","lc","impl"),
            ("ibf","bd","uses"),("bd","bref","uses"),("bref","bs","uses"),
            ("ibqb","ibq","creates"),("iic","iccf","uses"),
        ])

def gen_runtime():
    gen_module("runtime", "garganttua-runtime - Workflow Engine", 1000, 650,
        [   # Core
            ("irt","IRuntime<I,O>",80,80,190,50,"iface"),
            ("irts","IRuntimes",300,80,160,50,"iface"),
            ("irr","IRuntimeResult<O>",500,80,200,45,"iface"),
            # Execution
            ("irctx","IRuntimeContext",80,190,200,45,"iface"),
            ("ire","IRuntimeExecutor",320,190,190,45,"iface"),
            # Steps
            ("irs","IRuntimeStep",80,300,170,50,"iface"),
            ("irsc","IRuntimeStepCatch",300,300,200,45,"iface"),
            ("irsoe","IRuntimeStepOnException",540,300,250,45,"iface"),
            # Binders
            ("irsmb","IRuntimeStepMethodBinder",80,400,260,45,"iface"),
            ("irsfb","IRuntimeStepFallbackBinder",380,400,270,45,"iface"),
            # Builder
            ("rb","RuntimeBuilder",80,510,180,45,"class"),
            # Annotations
            ("ard","@RuntimeDefinition",300,510,190,35,"annot"),
            ("ast","@Step",510,510,100,35,"annot"),
            ("aio","@Input / @Output",640,510,160,35,"annot"),
            ("acatch","@Catch / @FallBack",300,565,200,35,"annot"),
        ],
        [
            ("irts","irt","uses"),("irt","irr","creates"),
            ("irt","irctx","uses"),("ire","irs","uses"),
            ("irs","irsc","uses"),("irsc","irsoe","uses"),
            ("rb","irt","creates"),
        ])

def gen_mapper():
    gen_module("mapper", "garganttua-mapper - Object Mapping", 700, 450,
        [("im","IMapper<S,T>",50,80,180,50,"iface"),
         ("mc","MapperConfiguration",280,80,210,45,"class"),
         ("mr","MappingRule",280,170,170,45,"class"),
         ("md","MappingDirection",500,80,170,40,"enum"),
         ("imre","IMappingRuleExecutor",50,170,220,45,"iface"),
         ("aomr","@ObjectMappingRule",50,280,200,35,"annot"),
         ("afmr","@FieldMappingRule",280,280,190,35,"annot"),
         ("exc","MapperException",500,280,170,40,"class")],
        [("im","mc","uses"),("im","mr","uses"),("imre","mr","uses"),
         ("mr","md","uses")])

def gen_expression():
    gen_module("expression", "garganttua-expression - ANTLR4 Expression Language", 1200, 750,
        [   # Context
            ("iec","IExpressionContext",80,80,220,50,"iface"),
            ("ec","ExpressionContext",80,170,210,45,"class"),
            ("ecb","ExpressionContextBuilder",80,250,250,45,"class"),
            # Expression
            ("iexp","IExpression<R,S>",400,80,200,50,"iface"),
            ("exp","Expression<R>",400,170,180,45,"class"),
            # Nodes
            ("ien","IExpressionNode<R,S>",650,80,230,50,"iface"),
            ("el","ExpressionLeaf<R>",650,170,200,45,"class"),
            ("en","ExpressionNode<R>",650,250,200,45,"class"),
            ("cen","ContextualExpressionNode<R>",650,330,270,45,"class"),
            ("dfn","DynamicFunctionNode<R>",650,410,240,45,"class"),
            # Factory
            ("enf","ExpressionNodeFactory<R,S>",400,330,260,45,"class"),
            # Functions
            ("sel","StandardExpressionLeafs",80,400,230,40,"class"),
            ("exprs","Expressions (cast, ...)",80,460,220,40,"class"),
            # ANTLR4
            ("g4","Expression.g4 (ANTLR4)",400,510,230,40,"note"),
            ("vis","ExpressionVisitor",400,580,200,45,"class"),
            # Annotations
            ("ael","@ExpressionLeaf",80,560,170,35,"annot"),
            ("aen","@ExpressionNode",270,560,180,35,"annot"),
            ("aex","@Expression",80,620,150,35,"annot"),
        ],
        [
            ("ec","iec","impl"),("ecb","ec","creates"),
            ("exp","iexp","impl"),("exp","ien","uses"),
            ("el","ien","impl"),("en","ien","impl"),("cen","ien","impl"),("dfn","ien","impl"),
            ("enf","ien","creates"),("ec","enf","uses"),
            ("vis","g4","uses"),("vis","enf","uses"),
        ],
        notes=[("n_dyn","enableDynamicFunctions()\nopts in to DynamicFunctionNode\nfor script user-defined functions",930,400,220,55)])

def gen_bootstrap():
    gen_module("bootstrap", "garganttua-bootstrap - Application Bootstrapping", 600, 350,
        [("iab","IApplicationBootstrap",150,70,240,50,"iface"),
         ("ab","ApplicationBootstrap",150,170,230,45,"class"),
         ("iic","IInjectionContext",150,270,210,45,"iface")],
        [("ab","iab","impl"),("ab","iic","uses")])

def gen_script():
    gen_module("script", "garganttua-script - Scripting Engine", 1200, 850,
        [   # Entry
            ("main","Main (CLI)",50,80,150,45,"class"),
            ("isc","IScript",250,80,140,45,"iface"),
            # Context
            ("sctx","ScriptContext",50,180,180,45,"class"),
            ("sec","ScriptExecutionContext",280,180,230,45,"class"),
            # Parsing
            ("g4","Script.g4 (ANTLR4)",50,290,200,40,"note"),
            ("bep","BlockExpressionPreprocessor",300,290,260,45,"class"),
            ("snv","ScriptNodeVisitor",600,290,210,45,"class"),
            # Runtime
            ("srs","ScriptRuntimeStep",50,400,210,50,"class"),
            # Nodes
            ("sb","StatementBlock",350,400,180,45,"class"),
            ("sf","ScriptFunction",350,480,180,45,"class"),
            ("fdn","FunctionDefNode",560,400,180,45,"class"),
            ("isn","IScriptNode",560,480,160,45,"iface"),
            # Built-in functions
            ("sfn","ScriptFunctions",50,560,200,45,"class"),
            ("cff","ControlFlowFunctions",50,630,220,45,"class"),
            ("rtf","RetryFunctions",300,560,180,45,"class"),
            ("syncf","SyncFunctions",300,630,180,45,"class"),
            ("timef","TimeFunctions",510,560,180,45,"class"),
            # Key concepts
            ("n_inc","include() + execute_script()\n+ script_variable()\nfor script composition",750,80,210,60,"note"),
            ("n_fn","User-defined functions:\nname = (params) => (body)\nScope isolation via save/restore",750,180,220,60,"note"),
            ("n_if","if(cond, thenBlock, elseBlock)\nLazy StatementBlock evaluation",750,280,220,45,"note"),
        ],
        [
            ("sctx","isc","impl"),("sctx","sec","uses"),
            ("snv","g4","uses"),("snv","bep","uses"),
            ("srs","sb","uses"),("srs","sf","uses"),("srs","fdn","uses"),
            ("sf","isn","impl"),("fdn","isn","impl"),
            ("main","sctx","uses"),
        ],
        groups=[
            ("g_parse","Parsing",40,265,830,85),
            ("g_fn","Built-in Functions",40,535,670,150),
        ])

def gen_console():
    gen_module("console", "garganttua-console - Interactive REPL", 700, 450,
        [("cm","ConsoleMain",250,70,200,50,"class"),
         ("jl","JLine Terminal",100,180,180,45,"class"),
         ("tc","Tab Completion",320,180,180,45,"class"),
         ("hist","History (~/.garganttua_script_history)",100,280,340,40,"note"),
         ("sctx","ScriptContext",100,370,180,45,"class"),
         ("ectx","ExpressionContext",320,370,200,45,"class"),
         ("n_repl","Built-in: help(), vars(),\nclear(), load(), man(),\nsyntax(), exit(), quit()",520,70,190,65,"note")],
        [("cm","jl","uses"),("cm","tc","uses"),("cm","sctx","uses"),("cm","ectx","uses")])

def gen_workflow():
    gen_module("workflow", "garganttua-workflow - Workflow Orchestration DSL", 1200, 850,
        [   # Builder DSL
            ("wb","WorkflowBuilder",80,80,200,50,"class"),
            ("wsb","WorkflowStageBuilder",80,170,220,45,"class"),
            ("wscb","WorkflowScriptBuilder",80,260,230,45,"class"),
            # Generator
            ("sg","ScriptGenerator",400,80,200,50,"class"),
            # Execution
            ("wf","Workflow",400,170,160,45,"class"),
            ("iwf","IWorkflow",400,250,160,45,"iface"),
            # I/O
            ("wi","WorkflowInput",650,80,170,45,"record"),
            ("wr","WorkflowResult",650,170,170,45,"record"),
            ("weo","WorkflowExecutionOptions",650,250,250,45,"record"),
            # Introspection
            ("wd","WorkflowDescriptor",650,340,200,45,"record"),
            # Code actions
            ("ca","CodeAction",400,340,150,40,"enum"),
            # Header
            ("shp","ScriptHeaderParser",80,400,200,45,"class"),
            ("sh","ScriptHeader",320,400,160,45,"record"),
            # Models
            ("wst","WorkflowStage",80,510,170,45,"record"),
            ("wsc","WorkflowScript",280,510,170,45,"record"),
            # Flow notes
            ("n_inc","Include mode:\ninclude() -> execute_script()\n-> script_variable()",80,620,230,60,"note"),
            ("n_inl","Inline mode:\n(...) group wrapping\nfor function scope isolation",350,620,220,55,"note"),
            ("n_when","when() conditions:\nif(cond, block, 0)\nStage + script combined with and()",620,620,240,55,"note"),
        ],
        [
            ("wb","wsb","creates"),("wsb","wscb","creates"),
            ("wb","sg","uses"),("wb","wf","creates"),
            ("wf","iwf","impl"),("wf","wr","creates"),
            ("sg","wst","uses"),("sg","wsc","uses"),
            ("sg","ca","uses"),("shp","sh","creates"),
            ("wf","wi","uses"),("wf","weo","uses"),("wf","wd","creates"),
        ],
        groups=[
            ("g_build","Builder DSL",70,55,250,270),
            ("g_exec","Execution",390,55,180,260),
            ("g_io","Input / Output / Introspection",640,55,270,345),
        ])


# ═══════════════════════════════════════════════════════════════════════════
#  MAIN
# ═══════════════════════════════════════════════════════════════════════════
if __name__ == "__main__":
    print("Generating draw.io diagrams...")
    gen_global()
    gen_commons()
    gen_dsl()
    gen_supply()
    gen_lifecycle()
    gen_mutex()
    gen_reflection()
    gen_runtime_reflection()
    gen_condition()
    gen_execution()
    gen_crypto()
    gen_configuration()
    gen_injection()
    gen_runtime()
    gen_mapper()
    gen_expression()
    gen_bootstrap()
    gen_script()
    gen_console()
    gen_workflow()
    print("Done! 20 diagrams generated.")
