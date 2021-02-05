#include<bits/stdc++.h>
using namespace std;
int main()
{
	int n,m,i,j,k,f,i1,j1;
	cin>>n>>m;
	for(i=n;i<=m;i++)
	{
		i1=i;
		if(i<0)
			i=-1*i;
		f=0;
		if(i==0 || i==1)
		{
			f=1;
			continue;
		}
		for(k=2;k*k<=i;k++)
		{
			if(i%k==0)
			{
				f=1;
				break;
			}
		}
		if(f==0)
			break;
	}
	if(f==1)
		i1=0;
	for(j=m;j>=n;j--)
	{
		j1=j;
		if(j<0)
			j=-1*j;
		f=0;
		if(j==1 || j==0)
		{
			f=1;
			continue;
		}
		for(k=2;k*k<=j;k++)
		{
			if(j%k==0)
			{
				f=1;
				break;
			}
		}
		if(f==0)
			break;
	}
	if(f==1)
		j1=0;
	cout<<abs(i1+j1)<<"\n";
	cout<<i1+j1;
}
