# Diagramas del SGA

| Archivo | Contenido |
|---|---|
| `Diagramas_SGA.pdf` | Los 6 diagramas en un solo PDF (vectorial, se puede ampliar) |
| `P_diagrama_paquetes` | Arquitectura por capas (paquetes y dependencias) |
| `A_diagrama_clases_dominio` | **Diagrama de clases**: modelo de dominio |
| `B_diagrama_clases_servicios` | **Diagrama de clases**: fachada `Alojamiento`, reportes, carga de datos |
| `C_diagrama_clases_gui` | **Diagrama de clases**: interfaz Swing |
| `D_diagrama_estructuras_propias` | **Diagrama de estructuras propias** (genéricas) y su uso en el SGA |
| `E_diagrama_excepciones` | Excepciones propias y dónde se lanzan |

Cada diagrama existe en `.png`, `.svg`, `.pdf` y como fuente `.dot` (Graphviz).

Para regenerarlos después de cambiar el código: `python3 gen.py` (requiere Graphviz `dot`).
