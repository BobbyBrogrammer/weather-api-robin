# Smart Reseassistent
### Individuell Labb 2 — Feltolerant backend med Spring Boot och externa API:er

---

## Vad är det här projektet?

Det här är min individuella labb 2 där jag byggt en backend-tjänst.
Idén är enkel: du skickar in en stad, och systemet hämtar aktuellt väder för den 
staden och returnerar aktivitetsrekommendationer baserat på vädret.
Regnar det? Då föreslår jag ett museum. Soligt? Då passar det bättre med en park.

Det som jag är extra nöjd med är att systemet är feltolerant, även om ett externt 
API är nere så kraschar inte min applikation.
Den returnerar istället reservdata så att användaren alltid får ett svar.

---

## Vad är projektet byggt med?

- **Java 21**
- **Spring Boot 4.0.6**
- **Spring WebFlux** (WebClient för icke-blockerande HTTP-anrop)
- **Resilience4J** (Circuit Breaker, TimeLimiter, Retry)
- **Lombok** (för att slippa boilerplate-kod)
- **MockWebServer** (OkHttp) för integrationstester
- **JUnit 5** för testerna

---

## Externa API:er

Jag använder mig av två externa API:er:

| API | Används till | Autentisering |
|---|---|---|
| [WeatherAPI](https://www.weatherapi.com/) | Hämtar aktuellt väder för en stad | API-nyckel som Query Parameter |
| [Geoapify](https://www.geoapify.com/) | Hämtar aktiviteter och platser | API-nyckel som Bearer Token i Authorization-headern |

Anledningen till att de autentiserar på olika sätt är medvetet, 
det var ett krav i uppgiften att demonstrera två olika metoder.

---

## Endpoint

```
GET /api/recommendations?location={city}
```

**Exempel:**
```
GET http://localhost:8080/api/recommendations?location=stockholm
```

**Svar:**
```json
{
  "city": "Stockholm",
  "weather": "Partly Cloudy",
  "activities": [
    {
      "name": "Skansen",
      "address": "Djurgårdsvägen 49",
      "category": "leisure.park"
    },
    ...
  ]
}
```

### Städer som stöds
Jag har lagt in koordinater för följande städer så att Geoapify söker på rätt plats:
- Stockholm
- Göteborg (gothenburg / göteborg / goteborg)
- Malmö (malmö / malmo)
- Kungälv (kungälv / kungalv)

Skriver man in en annan stad får man fortfarande ett svar, men med generella 
reservaktiviteter.

---

## Väderbaserad kategorimappning

Beroende på vädret väljer systemet olika kategorier att söka aktiviteter i:

| Väder | Kategori |
|---|---|
| Regn, duggregn, åska | `entertainment.museum` |
| Snö | `catering.cafe` |
| Soligt, klart | `leisure.park` |
| Allt annat | `entertainment` |

---

## Arkitektur

Jag har försökt följa Separation of Concerns genom hela projektet:

```
RecommendationController
        ↓
RecommendationService
        ↓              ↓
WeatherClient    ActivityClient
        ↓              ↓
   WeatherAPI      Geoapify
```

- **Controller** — tar emot HTTP-anropet och skickar vidare
- **Service** — kopplar ihop väder och aktiviteter med `flatMap()`
- **WeatherClient** — hämtar väderdata, hanterar fel och retry
- **ActivityClient** — hämtar aktiviteter baserat på stad och väder, hanterar 
fel och retry

---

## Feltolerans (Resilience4J)


### 1. Retry med exponentiell backoff
Om ett API-anrop misslyckas försöker systemet automatiskt upp till **3 gånger** med 
exponentiell väntetid mellan försöken (1s → 2s → 4s). Det skyddar mot tillfälliga 
nätverksstörningar.

### 2. Circuit Breaker
Om felen fortsätter bryter systemet kretsen (**OPEN**) för att inte överbelasta det 
externa API:et.
- Rullande fönster: **10 anrop**
- Öppnar vid: **50% felfrekvens**
- Väntar i: **10 sekunder** innan den går till **HALF-OPEN** och testar igen

### 3. TimeLimiter
Varje anrop har en maximal väntetid på **5 sekunder**. Tar det längre tid än så 
avbryts anropet och fallbacken aktiveras.

### Fallback-strategier

**Väder-fallback:** Om WeatherAPI är nere returneras standardvädret `"Sunny"` vilket 
gör att aktivitets-API:et ändå kan ge relevanta tips.

**Aktivitets-fallback:** Om Geoapify är nere (eller Circuit Breaker är OPEN) 
returneras en hårdkodad lista med stadsspecifika aktiviteter. Systemet går aldrig ner 
helt.

| Stad | Fallback-aktiviteter |
|---|---|
| Stockholm | Skansen, Vasa museet, Fotografiska, Gröna Lund, Café Pascal |
| Göteborg | Liseberg, Universeum, Maritiman, Slottsskogen, Café Husaren |
| Malmö | Malmö Museer, Kungsparken, Disgusting Food Museum, Folkets Park, Espresso House |
| Kungälv | Bohus Fästning, Nordmanna Bowling, Mimers Teater, Kareby Hembygdsgård, GolfOasen |

---

## Kom igång

### Förutsättningar
- Java 21
- Maven
- API-nycklar för WeatherAPI och Geoapify

### API-nycklar
Skapa filen `src/main/resources/application-local.properties` (den är gitignorerad 
och ska aldrig committas):

```properties
weather.api.key=DIN_WEATHERAPI_NYCKEL
activity.api.key=DIN_GEOAPIFY_NYCKEL
```

### Starta applikationen
```bash
mvn spring-boot:run
```

Applikationen startar på `http://localhost:8080`

---

## Tester

Jag har skrivit integrationstester med **MockWebServer** som simulerar nätverksfel 
och timeouts för att bevisa att felhanteringen fungerar som den ska.

### Kör testerna
```bash
mvn test
```

### Vad testerna bevisar

**WeatherClientTest (2 tester):**
- Att korrekt väderdata returneras när API:et svarar normalt
- Att fallbackvärdet `"Sunny"` returneras när API:et inte svarar i tid

**ActivityClientTest (3 tester):**
- Att aktiviteter returneras när API:et svarar normalt
- Att stadsspecifik fallback (t.ex. Skansen för Stockholm) returneras vid timeout
- Att Circuit Breaker går till **OPEN** och returnerar fallback utan att ens göra ett 
HTTP-anrop

---

## Säkerhet

Mina riktiga API-nycklar lagras **aldrig** i `application.properties` eller i Git.
Filen `application-local.properties` som innehåller de riktiga nycklarna är tillagd i
`.gitignore`. `application.properties` innehåller bara platshållaren 
`YOUR_API_KEY_HERE` för att dokumentera vilka properties som krävs.

---

## Dokumentation

Screenshots som dokumenterar att systemet fungerar finns i `src/main/resources/docs/`.

---

*Byggt av Robin Lindholm — Individuell Labb 2*
