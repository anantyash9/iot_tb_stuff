import json
import requests
import base64
with open('secrets.json') as f:
    data = json.load(f)

uname = data["uname"]
password = data["password"]
url = data["url"]
data = {"username": uname, "password": password}
r = requests.post(url, json=data)
token = (eval(r.text))["token"]

headers={'Content-Type': 'application/json',
           'X-Authorization': "Bearer "+token
           }


def change_pin_state(pin,state,tb_controller_id):
    rpc_url="https://demo.thingsboard.io/api/plugins/rpc/oneway/"+tb_controller_id
    json={
    "method": "setGpioStatus",
    "params": {"pin": pin, "enabled": state}}
    r=requests.post(rpc_url, headers=headers, json=json)

def forward(tb_controller_id):
    change_pin_state(3,True,tb_controller_id)
    change_pin_state(3,False,tb_controller_id)

def revers(tb_controller_id):
    change_pin_state(5,True,tb_controller_id)
    change_pin_state(5,False,tb_controller_id)

def set_name_plate(id,name,image,tb_display_id):
    img=base64.encodebytes(image).decode("utf-8")
    rpc_url="https://demo.thingsboard.io/api/plugins/rpc/oneway/"+tb_display_id
    json={
    "method": "setIdStatus",
    "params": {"id": id, "name": name ,"image":img}}
    r=requests.post(rpc_url, headers=headers, json=json)

# while(True):
#     d=input("enter direction")
#     if d=='1':
#         forward()
#     elif d=='0':
#         revers()
#     else:
#         exit()    
     
forward('e6821290-473e-11ea-b204-21c239be13bb')

with open('pic.jpg', mode='rb') as file:
    img = file.read()

set_name_plate("738608","AYP",img,'22c16e20-48f9-11ea-b757-833b99914e57')



