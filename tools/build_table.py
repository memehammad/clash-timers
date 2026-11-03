import json,glob,sys
src,out=sys.argv[1],sys.argv[2]
def secs(t):
    if isinstance(t,dict): return t.get('days',0)*86400+t.get('hours',0)*3600+t.get('minutes',0)*60+t.get('seconds',0)
    if isinstance(t,(int,float)): return int(t)
    return None
table={}
for f in sorted(glob.glob(src+'/**/*.json',recursive=True)):
    try: d=json.load(open(f))
    except Exception: continue
    def walk(o):
        if isinstance(o,dict):
            if isinstance(o.get('dataId'),int) and isinstance(o.get('name'),str) and o['dataId'] not in table:
                times={}
                for lv in o.get('levels') or []:
                    if not isinstance(lv,dict) or 'level' not in lv: continue
                    for k in ('buildTime','upgradeTime','researchTime'):
                        s=secs(lv.get(k))
                        if s: times[str(lv['level'])]=s; break
                e={'n':o['name']}
                if times: e['t']=times
                table[o['dataId']]=e
            for v in o.values(): walk(v)
        elif isinstance(o,list):
            for v in o: walk(v)
    walk(d)
json.dump({str(k):table[k] for k in sorted(table)},open(out,'w'),separators=(',',':'),ensure_ascii=False)
print(len(table),'entries')
