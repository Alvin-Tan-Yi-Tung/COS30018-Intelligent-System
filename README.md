# COS30018-Intelligent-System

# 🚗 Automated Negotiation System - Multi-Agent Platform

A JADE-based multi-agent system for automated and manual car price negotiations between buyers and dealers, with a broker facilitating matches and earning commissions.

## 📋 Table of Contents
- [Features](#-features)
- [Agents](#-agents)
- [Installation](#-installation)
- [Usage](#-usage)

## 🌟 Features

- **Integrated Manual and Automated Negotiation**:
  - Fully automated agent-to-agent negotiations
  - Human-in-the-loop manual negotiations via GUI
- **Real-time Visualization**:
  - Interactive sequence diagrams
  - Chat interfaces for manual negotiations
- **Commission Tracking**:
  - Separate tracking for automated/manual deals
  - Real-time commission updates
- **Advanced Matching**:
  - Broker-mediated dealer matching
  - Price-based suitability filtering

## 🤖 Agents

### Core Agents:
- **BrokerAgent**: Central matchmaker that:
  - Maintains dealer inventory
  - Processes buyer requests
  - Calculates commissions (RM500 per deal)
- **SnifferAgent**: Monitors all system communications

### Buyer Agents:
- **Automated (A.Buyer*)**: 
  - Pre-configured budgets/offers
  - Automatic negotiation
- **Manual (M.Buyer*)**: 
  - GUI-configurable parameters
  - Human-controlled negotiation

### Dealer Agents:
- **Automated (A.Dealer*)**:
  - Fixed pricing
  - Algorithmic responses
- **Manual (M.Dealer*)**:
  - Adjustable listings
  - Human-managed offers

## 💻 Installation

1. **Prerequisites**:
- Java JDK 8+
- Ecplise JADE platform

2. **Setup**:
- Create new java project and name it what you want
- Right click the project folder, select ```Build Path```, then select ```Configure Build Path...```
- Select ```Add JARs..```, put ```jade.jar``` and ```jadeExamples.jar```, then select ```OK```  
- Under ```src``` folder, create a ```IntelligentProject``` package
- Import all the java files into the package and save it as ```UTF-8```

3. **Run**:
- Right click the ```Main.java```
- Select ```Run As```
- Click ```1 Java Application```
![image](https://github.com/user-attachments/assets/997ed7d3-0978-4327-8ef9-db4b494fa8f7)


## 🖥 Usage
### Creating Agents:
1. **Manual Buyers**:
  - Click "```+ Add Buyer Agent```"
  - Enter: ```Car Type```, ```Initial Offer```, ```Max Budget```
  - View matches in ```M.Buyer1``` tab

2. **Manual Dealers**:
  - Click "```+ Add Dealer Agent```"
  - Enter: ```Car Type```, ```Listing Price```
  - Receive requests in ```M.Dealer1``` tab

3. **Negotiation Flow**:
  - Buyers query broker for matching dealers
  - Select dealer and ```Send Request```
  - Dealers receive requests and can:
    - ```Accept``` → Start chat negotiation
    - ```Reject``` → Remove request

4. **Mutual acceptance confirms deal**
