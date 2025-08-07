## Zaktualizowany Opis Pluginu do Serwera MMO RPG

### Główne założenia:

- Plugin tworzony na wersję Paper 1.20.1.
- Generowanie dynamicznych sfer (stref), gracze kupują dostęp.
- Każda sfera generowana losowo z ustalonych typów.
- Limit czasowy: 10 minut w sferze.
- Kryształy są główną walutą (z rud oraz specjalnych stref kryształowych).

### System Kilofów i Staminy:

- Gracze ulepszają kilofy (drewno → kamień → żelazo itd.), co zwiększa szybkość kopania.
- Każdy kilof ma **wytrzymałość** – określa ile razy można nim kopać, zanim się zniszczy.
- Wprowadzony zostaje system **staminy**:
  - Gracz ma domyślnie 100 staminy.
  - Wejście do jednej sfery zużywa 10 staminy.
  - Stamina resetuje się co 12h lub może być zwiększana przez ulepszenia.
- Ulepszanie kilofów realizowane będzie przez zewnętrzny plugin do craftingu.
- Będzie dostępna funkcja **Quick Sell** – automatyczna sprzedaż rud za pieniądze (\$).

### Typy Sfer:

1. **Ore Sphere** – Głównie rudy.
2. **Treasure Sphere** – Skrzynie z lootem.
3. **Vegetation Sphere** – Roślinność, alchemia.
4. **Mob Sphere** – Potwory i unikalne dropy.
5. **Boss Sphere** – Walka z bossem, specjalne nagrody.
6. **Special Event Sphere** – NPC eventy (np. Jewel Trader, Blacksmith of Souls).
7. **Puzzle Sphere** – Zagadki logiczne.
8. **Crystal and Dust Sphere** – Specjalna strefa wydobycia kryształów i pyłu.

### Liczba wariantów typów sfer:

| Typ Sfery               | Liczba wariantów |
| ----------------------- | ---------------- |
| Ore Sphere              | 8                |
| Treasure Sphere         | 4                |
| Vegetation Sphere       | 5                |
| Mob Sphere              | 6                |
| Puzzle Sphere           | 4                |
| Crystal and Dust Sphere | 3                |
| Boss Sphere             | 1                |
| Special Event Sphere    | 1                |

Każda sfera (poza unikalnymi) ma wiele wersji strukturalnych generowanych losowo przy wejściu gracza, co pozwala utrzymać świeżość rozgrywki.

### Hierarchia Rud i ich Wytrzymałość (liczba uderzeń):

#### Black Ore (Coal-based):

- Hematite: 40
- Black Spinel: 70
- Black Diamond: 120

#### Metallic Ore (Iron-based):

- Magnetite: 50
- Silver: 90
- Osmium: 140

#### Azure Ore (Lapis-based):

- Azurite: 60
- Tanzanite: 100
- Blue Sapphire: 150

#### Crimson Ore (Redstone-based):

- Carnelian: 65
- Red Spinel: 115
- Pigeon Blood Ruby: 175

#### Golden Ore (Gold-based):

- Pyrite: 75
- Yellow Topaz: 125
- Yellow Sapphire: 180

#### Verdant Ore (Emerald-based):

- Malachite: 90
- Peridot: 130
- Tropiche Emerald: 200

#### Prismatic Ore (Diamond-based):

- Danburite: 100
- Goshenite: 175
- Cerussite: 300

### Mechanika Wydobycia:

- Gracz ma 10 minut na wydobycie jak największej liczby rud.
- Możliwość ulepszania kilofów redukująca liczbę uderzeń i zwiększająca szybkość wydobycia.
- Drop rzadkich materiałów zależny od poziomu ulepszenia kilofa.

### Szanse Generowania:

#### Szansa na bazowy typ rudy:

- Black Ore (Coal-based): 25%
- Metallic Ore (Iron-based): 20%
- Azure Ore (Lapis-based): 15%
- Crimson Ore (Redstone-based): 12%
- Golden Ore (Gold-based): 10%
- Verdant Ore (Emerald-based): 10%
- Prismatic Ore (Diamond-based): 8%

#### Szansa na rzadkość w obrębie typu:

- Type I – Common: 70%
- Type II – Rare: 20%
- Type III – Epic: 10%

#### Szansa na typ sfery:

- Ore Sphere: 45%
- Treasure Sphere: 11%
- Vegetation Sphere: 15%
- Mob Sphere: 15%
- Boss Sphere: 3%
- Special Event Sphere: 5%
- Puzzle Sphere: 7%
- Crystal and Dust Sphere: 5%

### Wielkość Sfer:

| Typ Sfery                | Promień (bloków) | Średnica | Uwagi                                                       |
| ------------------------ | ---------------- | -------- | ----------------------------------------------------------- |
| **Treasure Sphere**      | 7–8              | 15–17    | Kompaktowa, idealna dla skrzyń z lootem.                    |
| **Vegetation Sphere**    | 9–11             | 19–23    | Roślinność, zioła, grzyby – dobra przestrzeń.               |
| **Ore Sphere**           | 10–12            | 21–25    | Balans między ilością rud a przestrzenią do poruszania się. |
| **Crystal and Dust**     | 11–13            | 23–27    | Więcej miejsca na dekoracje i efekty magiczne.              |
| **Mob Sphere**           | 13–14            | 27–29    | Większa przestrzeń do walki z mobami.                       |
| **Puzzle Sphere**        | 12–14            | 25–29    | Parkoury, zagadki, przełączniki, ukryte ścieżki.            |
| **Boss Sphere**          | 15–18            | 31–37    | Arena do walki z bossem, efektowna i przestronna.           |
| **Special Event Sphere** | 12–14            | 25–29    | NPC, stoiska eventowe, interaktywne lokacje.                |

### Zasady Funkcjonowania Sfer:

- Gracz **nie może wrócić** do sfery po jej opuszczeniu.
- Sfera **znika bezpowrotnie** po czasie lub opuszczeniu.
- Gracz **może zabierać** zdobyte przedmioty ze sfery.
- Tier 1 sfery są darmowe — zużycie kilofa jako koszt.
- Wyższe tier’y mogą wymagać np. **biletu premium**.

### Integracja i Questy:

- Plugin korzysta z Vault (ekonomia).
- API będzie raportować ukończenie sfery (np. do systemu zadań).
- System **28 jednorazowych questów** z nagrodami będzie powiązany z sferami.

### Proces losowania sfery:

1. Wylosowanie typu sfery.
2. Wybór layoutu (preset).
3. Wybór bazy rud.
4. Wybór typu rudy (Common, Rare, Epic).

### System presetów i schematów:

- Do budowy sfer używany jest system schematów `.schem` z WorldEdit / FAWE.
- Presety są tworzone ręcznie w Minecraft i zapisywane przez komendę:
  ```
  //copy
  //schem save ore_sphere_01
  ```
- W miejscach, gdzie mają pojawić się rudy, należy wstawić **blok znacznikowy** (np. zielona wełna).
- Po wczytaniu schematu przez FAWE plugin:
  - Rozpoznaje znaczniki,
  - Zamienia je na odpowiednie typy rud zgodnie z losowaniem.
- Schematy będą przechowywane np. w `/plugins/yourplugin/schematics/`.
- Po zakończeniu czasu (lub wyjściu gracza), sfera zostaje usunięta
