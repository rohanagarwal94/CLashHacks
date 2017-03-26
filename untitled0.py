# -*- coding: utf-8 -*-
"""
Created on Sun Mar 26 04:05:27 2017

@author: lenovo
"""
#18a7b385ee3433d4d37492e4ce23d45a
import requests
import json
import numpy as np
from nltk import tokenize
import http.client, urllib.request, urllib.parse, urllib.error, base64

city="delhi ncr"
word="Fried Rice"
url = "https://developers.zomato.com/api/v2.1/cities?q="+city

headers = {"Content-type": "application/json",
           "user-key":"18a7b385ee3433d4d37492e4ce23d45a"}
r  = requests.get(url,headers=headers)

jData = json.loads(r.content)
print(jData)
l=jData['location_suggestions']
entity_id=0
for i in range(len(l)):
    if(l[i]['country_id']==1):
        entity_id=l[i]['id']
        
print(entity_id)
cuisine="Chinese"
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
            user_restaurants_ids.append(restaurant['R']['res_id'])
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
    return (restaurant_rating/5+review_rating)/2

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
            j = json.loads(data.content)
            print(j['documents'][0]['score'])
            conn.close()
            return ['documents'][0]['score']
    except Exception as e:
        print("[Errno {0}] {1}".format(e.errno, e.strerror))
    

actual_ratings=[]

for i in range(len(reviews)):
    if(reviews[i]==""):
        actual_ratings.append(find_rating(user_restaurants_ratings[i],0.3))
    else:
        actual_ratings.append(find_rating(user_restaurants_ratings[i],analyse_review(reviews[i])))
        
a=np.column_stack((user_restaurants_ids, actual_ratings))
a = sorted(a, key=lambda a_entry: a_entry[1]) 
print(a)
