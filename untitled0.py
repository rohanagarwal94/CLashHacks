# -*- coding: utf-8 -*-
"""
Created on Sun Mar 26 04:05:27 2017

@author: lenovo
"""
#18a7b385ee3433d4d37492e4ce23d45a
import requests
import json
import re
import difflib
import json
import numpy as np
from nltk import tokenize
import http.client, urllib.request, urllib.parse, urllib.error, base64

#save the string from API call here
text = 'best red sauce pasta in medium budget'
city="delhi ncr"
'''
m = re.search('best (.+?) under', text)
if m:
    word = m.group(1)

with open('cuisine.json') as json_file:
    data = json.load(json_file)
    for p in data['dishes']:
        seq=difflib.SequenceMatcher(word=word.lower(), p['dish_name']=p['dish_name'].lower())
        if (seq.ratio>0.1):
            cuisine = p['cuisine']
            break

n = re.search('under (.+?) budget', text)
if m:
    budget = m.group(1)

if(budget == 'low'):
    price_range = 2
elif(budget == 'medium'):
    price_range = 3
else:
    price_range = 4

#low=2 medium =3 high =4 (price_range)
'''
#city="delhi ncr"
word="Fried Rice"
cuisine="Chinese"
url = "https://developers.zomato.com/api/v2.1/cities?q="+city

headers = {"Content-type": "application/json",
           "user-key":"78eba48da7a3f396db77cba175b53a9c"}
r  = requests.get(url,headers=headers)

jData = json.loads(r.content)
print(jData)
l=jData['location_suggestions']
entity_id=-1
for i in range(len(l)):
    if(l[i]['country_id']==1):
        entity_id=l[i]['id']
        
print(entity_id)
#cuisine="Chinese"
url2 = "https://developers.zomato.com/api/v2.1/cuisines?city_id="+str(entity_id)
r2  = requests.get(url2,headers=headers)
jData2 = json.loads(r2.content)
l2=jData2['cuisines']
cuisine_id=-1
for i in range(len(l2)):
    obj=l2[i]
    obj=obj['cuisine']
    if(obj['cuisine_name']==cuisine):
        cuisine_id=obj['cuisine_id']
print(cuisine_id)
'''
url3="https://developers.zomato.com/api/v2.1/search?entity_id=1&entity_type=city&start=0&cuisines=88&sort=rating&order=desc"
r3  = requests.get(url3,headers=headers)
jData3 = json.loads(r3.content)
restaurants=jData3['restaurants']
num_results=jData3['results_found']
'''
j=0
user_price=3

user_restaurants_ids=[]
user_restaurants_names=[]
user_restaurants_ratings=[]
while(True):
    url3="https://developers.zomato.com/api/v2.1/search?entity_id="+str(entity_id)+"&entity_type=city&start="+str(j)+"&cuisines="+str(cuisine_id)+"&sort=rating&order=desc"
    r3  = requests.get(url3,headers=headers)
    jData3 = json.loads(r3.content)
    restaurants=jData3['restaurants']
    num_results=jData3['results_shown']
    num=jData3['results_found']
    for i in range(len(restaurants)):
        restaurant=restaurants[i]['restaurant']
        if(restaurant['price_range']<=user_price):
            user_restaurants_ids.append(restaurant['id'])
            user_restaurants_names.append(restaurant['name'])
            user_restaurants_ratings.append(restaurant['user_rating']['aggregate_rating'])    
    j=j+num_results
    #print(num)
    #print(j)
    if(j==num or j>=400 or len(restaurants)==0):
        break

#user_restaurants_ratings=user_restaurants_ratings/5

print(user_restaurants_ratings)
            
selected_restaurants=np.column_stack((user_restaurants_ids,user_restaurants_ratings))
print(len(selected_restaurants))
reviews=[]
for i in range(len(selected_restaurants)):
    url4="https://developers.zomato.com/api/v2.1/reviews?res_id="+str(selected_restaurants[i][0])
    r4= requests.get(url4,headers=headers)
    try:
        jData4 = json.loads(r4.content)
    except Exception:
        print("empty review")
        reviews.append("")
    else:
        review=""
        for j in range(jData4['reviews_shown']):
            review=review+str(jData4['user_reviews'][j]['review']['review_text'])
        if(review!=""):
            lines=tokenize.sent_tokenize(review)
            ans=""
            for i in range(len(lines)):
                if word in lines[i] or word.lower() in lines[i]:
                    ans=ans+lines[i]
            reviews.append(ans)
print(reviews)

def find_rating(restaurant_rating, review_rating):
    return (float(restaurant_rating)/5+review_rating)/2

def analyse_review(review):
    headers = {
        'Content-Type': 'application/json',
        'Ocp-Apim-Subscription-Key': '61a10ca56bf249818d899fc4c4fb709f',
        }

    params = urllib.parse.urlencode({
            })
    body={
            "documents": [
                    {
                            "language": "en",
                            "id": "2",
                            "text": review
                                }
                    ]
            }
    try:
            conn = http.client.HTTPSConnection('westus.api.cognitive.microsoft.com')
            conn.request("POST", "/text/analytics/v2.0/sentiment?%s" % params, str(body), headers)
            response = conn.getresponse()
            data = response.read()
            j = json.loads(data)
            print(j['documents'][0]['score'])
            conn.close()
            return j['documents'][0]['score']
    except Exception as e:
        return 0.3
    

actual_ratings=[]

for i in range(len(reviews)):
    if(reviews[i]==""):
        actual_ratings.append(find_rating(user_restaurants_ratings[i],0.3))
    else:
        actual_ratings.append(find_rating(user_restaurants_ratings[i],analyse_review(reviews[i])))
        
a=np.column_stack((user_restaurants_ids, actual_ratings,user_restaurants_names))
a = sorted(a, key=lambda a_entry: a_entry[1]) 
k=-1*min(10,len(a)) - 1
print(a[:k:-1])
#for i in range(min(len(a),10)):