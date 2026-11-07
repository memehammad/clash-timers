<?xml version="1.0" encoding="UTF-8"?>
<!-- Only for people opening sitemap.xml in a browser; search engines ignore it. -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:s="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
  <xsl:output method="html" encoding="UTF-8"/>
  <xsl:template match="/">
    <html><head><title>Sitemap – Clash Timers</title><meta name="viewport" content="width=device-width, initial-scale=1"/>
      <style>body{font:16px/1.5 system-ui,sans-serif;margin:24px 16px;color:#171C1F;background:#F5FAFD}table{border-collapse:collapse;width:100%;max-width:900px}td,th{text-align:left;padding:8px;border-bottom:1px solid #C0C8CC;word-break:break-all}th{font-size:14px;color:#40484C}a{color:#0C6780}@media (prefers-color-scheme:dark){body{background:#0F1417;color:#DEE3E6}a{color:#89D0ED}td,th{border-color:#40484C}}</style>
    </head><body>
      <h1>Sitemap</h1>
      <p><xsl:value-of select="count(s:urlset/s:url)"/> pages. This file is for search engines; this view is just for humans.</p>
      <table><tr><th>Page</th><th>Languages</th><th>Last updated</th></tr>
        <xsl:for-each select="s:urlset/s:url">
          <tr><td><a href="{s:loc}"><xsl:value-of select="s:loc"/></a></td>
            <td><xsl:for-each select="xhtml:link[@hreflang!='x-default']"><xsl:value-of select="@hreflang"/><xsl:text> </xsl:text></xsl:for-each></td>
            <td><xsl:value-of select="s:lastmod"/></td></tr>
        </xsl:for-each>
      </table>
    </body></html>
  </xsl:template>
</xsl:stylesheet>
