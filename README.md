# Yelp Search based on food name
## 1. Introduction
Yelp is very useful for us to find a good restaurant. Typing the restaurant name or food name, we can easily find the positions, scores and reviews of related restaurants. However, searching based on food name always pumps out a huge list of restaurants, which makes users hard to decide which restaurant is better.  Although users can compare restaurants based on stars assigned to that restaurant, but these stars only score the restaurants, not a specific food. For example, a user is searching sushi for lunch. Both restaurant A and B has sushi. A is 4 stars, but B is 3 stars. From the score, the user may think that A is a better choice to eat sushi. But the fact turns out that B is more famous for its sushi and the only reason for A to score better than B is that A has better ramen than B. Therefore, food name based search in Yelp may not be helpful enough for user to make proper decision. As a result, in addition to scoring restaurants, it is also necessary to score different food for each restaurant.

The review system in Yelp can help. There are one or more reviews attached to each restaurant. These reviews provide a lot of information about what food is good, what food is not so tasty. If extracting average customer’s opinion on specific food in each restaurant is possible and building a database using these food name and corresponding score, then search based on food name will become possible.

Basically, my approach is using NLP to extract keywords from reviews and score each keyword for each restaurant. It can be generally separated into following steps:

A. Read business json data from yelp_academic_dataset_business.json and put them into table BUSINESS. (Use business_id as primary key.)

B. Read review json data from yelp_academic_dataset_review.json and put them them into table REVIEW. (Use review_id as primary key.)

C. After reading review data, extract keywords and corresponding sentiment score from each review. Create table KEYWORD ({keyword, business_id} is the primary key) and put in all the keywords.

D. When performing search,  query all the entries in table KEYWORD that has the keyword and sort based on the keyword sentiment score.
## 2. Approach
### 2.1. Build table BUSINESS 
The following is an example of JSON object from yelp_academic_dataset_business.json. Each JSON object records variable information about the business and is uniquely identified by business_id property. 
```
{"business_id": "5UmKMjUEUNdYWqANhGckJw", 
"full_address": "4734 Lebanon Church Rd\nDravosburg, PA 15034", 
… … … 
"type": "business"}
```
For efficiency of reading data, all these JSON objects are put into database. Considering the scalability and possible modification of data structure, it is more suitable to use unstructural database. Here, I use Google datastore to store these data. The ID is business_id. The other attribute is info which is the JSON object itself. The following shows one entry of the table BUSINESS.

| business_id (primary key) | info |
|---------------------------|------|
| oKpZRrSqiqr5DzrYBlHfsw    | {"business_id": "5UmKMjUEUNdYWqANhGckJw", "full_address": "4734 Lebanon Church Rd\nDravosburg, PA 15034", … … … "type": "business"} |

### 2.2. Build table REVIEW 
The following is an example of JSON object from yelp_academic_dataset_review.json. Each JSON object records variable information about the business and is uniquely identified by review_id property. 
```
{"votes": {"funny": 0, "useful": 0, "cool": 0}, 
"user_id": "Iu6AxdBYGR4A0wspR9BYHA", 
"review_id": "KPvLNJ21_4wbYNctrOwWdQ", 
"stars": 5, 
"date": "2014-02-13", 
"text": "Excellent food. Superb customer service. I miss the mario machines they used to have, but it's still a great place steeped in tradition.", 
"type": "review", 
"business_id": "5UmKMjUEUNdYWqANhGckJw"}
```
For efficiency of reading data, all these JSON objects are put into database. The ID is review_id. For convenience business_id is also defined as a property. The other attribute is info which is the JSON object itself. The following shows one entry of the table REVIEW.

|review_id (primary key)    | business_id | info |
|---------------------------|-------------|------|
|KPvLNJ21_4wbYNctrOwWdQ     | oKpZRrSqiqr5DzrYBlHfsw    | {"business_id": "5UmKMjUEUNdYWqANhGckJw", "full_address": "4734 Lebanon Church Rd\nDravosburg, PA 15034", … … … "type": "business"} |

### 2.3. Extract keyword and sentiment
Next step is to extract keywords and sentiment from these reviews. Luckily, some NLP tools can help do so. AlchemyLanguage (http://www.alchemyapi.com/products/demo/alchemylanguage) is such NLP tools which can extract a list of keywords along with sentiment (“positive”, “negative”, “neutral”) and corresponding possibility score from text. The following is one example result from that NLP API:

```
{
  "status": "OK",
  "usage": "By accessing AlchemyAPI or using information generated by AlchemyAPI, you are agreeing to be bound by the AlchemyAPI Terms of Use: http://www.alchemyapi.com/company/terms.html",
  "totalTransactions": "2",
  "language": "english",
  "text": "Walking in, it does seem like a throwback to 30 years ago, old fashioned menu board, booths out of the 70s, and a large selection of food. ",
  "keywords": [
    {
      "relevance": "0.968798",
      "sentiment": {
        "score": "0.489015",
        "type": "positive"
      },
      "text": "old fashioned menu"
    },
    {
      "relevance": "0.84218",
      "sentiment": {
        "score": "0.328499",
        "type": "positive"
      },
      "text": "large selection"
    },
   … … ...
}
```
It can be seen from the results that each keyword has its sentiment, either positive, negative, or neutral and the posibility score.  
Using these information, a table KEYWORD can be built. This table use a tuple {keyword, business_id} as primary key. The other attribute is sentiment which is a JSON object about total sentiment score on that keyword. The following is one example entry of the table:

| keyword | business_id | sentiment |
|---------|-------------|-----------|
| sushi | GitvoU8mPOLieMiiw503rg | {"positive":1.2, "negative":-0.2, "neutral": 0} |

The sentiment attribute is summary of all the NLP results of reviews of the restaurant. For example, there are three review mentioning “sushi” of a certain restaurant. One says “sushi in this restaurant tastes good.”, Another says “sushi in this restaurant is really tasty. ”  The last one says “sushi is good here, but the nearby japanese sushi bar is better.” So the sentiment for keyword “sushi” is respectively {“positive”, 0.7}, {“positive, 0.5”}, {“negative”, -0.2}. So the sentiment property in the table KEYWORD would be {"positive":1.2, "negative":-0.2,
"neutral": 0} as shown above. 

### 2.4. Search based on food name
After the completion of table KEYWORD, it is OK to perform search on it. For example, if searching “sushi”. Then the query string should be transformed into three format: “Sushi”, “SUSHI” and “sushi”. By scanning the database, a list of entries of that keyword would pop out. 

## 3. User Interface
The following is a simple web app to perform search based on food name.
<img src="https://github.com/Peleus1992/YelpSearchFoodName/blob/master/screenshot/1.png"/>
<img src="https://github.com/Peleus1992/YelpSearchFoodName/blob/master/screenshot/2.png"/>
<img src="https://github.com/Peleus1992/YelpSearchFoodName/blob/master/screenshot/3.png"/>
<img src="https://github.com/Peleus1992/YelpSearchFoodName/blob/master/screenshot/4.png"/>

## 4. Deliverable
This is simple heuristic project using NLP keyword extraction to improve the search engine of Yelp. Since the AlchemyLanguage has only 1000 requests upper limit every day, only a small group of restaurants is included. (Japanese restaurants at Waterloo, ON)

Here is the link to the source code: https://github.com/Peleus1992/YelpSearchBasedOnFoodName

Here is the link to the web app: https://yelpsearchfood.appspot.com/
